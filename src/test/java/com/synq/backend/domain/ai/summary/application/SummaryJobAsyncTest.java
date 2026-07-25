package com.synq.backend.domain.ai.summary.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.synq.backend.domain.ai.summary.domain.GeneratedSummary;
import com.synq.backend.domain.ai.summary.domain.MeetingContextReader;
import com.synq.backend.domain.ai.summary.domain.MeetingSummaryStore;
import com.synq.backend.domain.ai.summary.domain.RagContextReader;
import com.synq.backend.domain.ai.summary.domain.SummaryAiClient;
import com.synq.backend.domain.ai.summary.domain.SummaryContext;
import com.synq.backend.domain.ai.summary.domain.SummaryJob;
import com.synq.backend.domain.ai.summary.domain.SummaryJobStatus;
import com.synq.backend.domain.ai.summary.domain.SummaryJobStore;
import com.synq.backend.domain.ai.summary.domain.TranscriptReader;
import com.synq.backend.domain.ai.summary.mock.InMemoryMeetingSummaryStore;
import com.synq.backend.domain.ai.summary.mock.InMemorySummaryJobStore;
import com.synq.backend.domain.ai.summary.mock.MockMeetingContextReader;
import com.synq.backend.domain.ai.summary.mock.MockRagContextReader;
import com.synq.backend.domain.ai.summary.mock.MockTranscriptReader;
import com.synq.backend.global.apipayload.code.GeneralErrorCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(SummaryJobAsyncTest.AsyncTestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SummaryJobAsyncTest {

	@jakarta.annotation.Resource
	private MeetingSummaryService meetingSummaryService;

	@jakarta.annotation.Resource
	private RecordingSummaryJobStore jobStore;

	@jakarta.annotation.Resource
	private MeetingSummaryStore summaryStore;

	@jakarta.annotation.Resource
	private BlockingSummaryAiClient summaryAiClient;

	@Test
	void 비동기_요약_중에는_중복_요청을_차단하고_완료_상태로_전이한다() throws Exception {
		SummaryJob job = meetingSummaryService.request(1L);

		assertThat(summaryAiClient.awaitStarted()).isTrue();
		assertThat(jobStore.findById(job.id()).orElseThrow().status()).isEqualTo(SummaryJobStatus.PROCESSING);
		assertThatThrownBy(() -> meetingSummaryService.request(1L))
				.isInstanceOf(GeneralException.class)
				.extracting("code")
				.isEqualTo(GeneralErrorCode.CONFLICT);

		summaryAiClient.release();

		assertThat(waitForStatus(job.id())).isEqualTo(SummaryJobStatus.COMPLETED);
		assertThat(summaryStore.findLatestByMeetingId(1L)).isPresent();
	}

	@Test
	void 요약_실행_대기열이_가득차면_Job을_실패로_남기고_503을_반환한다() throws Exception {
		SummaryJob runningJob = meetingSummaryService.request(1L);
		assertThat(summaryAiClient.awaitStarted()).isTrue();

		assertThatThrownBy(() -> meetingSummaryService.request(2L))
				.isInstanceOf(GeneralException.class)
				.extracting("code")
				.isEqualTo(GeneralErrorCode.SERVICE_UNAVAILABLE);

		SummaryJob rejectedJob = jobStore.lastSavedJob();
		assertThat(rejectedJob.meetingId()).isEqualTo(2L);
		assertThat(rejectedJob.status()).isEqualTo(SummaryJobStatus.FAILED);
		assertThat(rejectedJob.errorMessage()).contains("대기열이 가득");

		summaryAiClient.release();
		assertThat(waitForStatus(runningJob.id())).isEqualTo(SummaryJobStatus.COMPLETED);
	}

	private SummaryJobStatus waitForStatus(java.util.UUID jobId) throws InterruptedException {
		for (int attempt = 0; attempt < 100; attempt++) {
			SummaryJobStatus status = jobStore.findById(jobId).orElseThrow().status();
			if (status == SummaryJobStatus.COMPLETED || status == SummaryJobStatus.FAILED) {
				return status;
			}
			Thread.sleep(10);
		}
		return SummaryJobStatus.FAILED;
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAsync(proxyTargetClass = true)
	static class AsyncTestConfig {

		@Bean(name = "summaryExecutor")
		Executor summaryExecutor() {
			ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
			executor.setCorePoolSize(1);
			executor.setMaxPoolSize(1);
			executor.setQueueCapacity(0);
			executor.initialize();
			return executor;
		}

		@Bean
		RecordingSummaryJobStore summaryJobStore() {
			return new RecordingSummaryJobStore();
		}

		@Bean
		MeetingSummaryStore meetingSummaryStore() {
			return new InMemoryMeetingSummaryStore();
		}

		@Bean
		TranscriptReader transcriptReader() {
			return new MockTranscriptReader();
		}

		@Bean
		MeetingContextReader meetingContextReader() {
			return new MockMeetingContextReader();
		}

		@Bean
		RagContextReader ragContextReader() {
			return new MockRagContextReader();
		}

		@Bean
		SummaryContextBuilder summaryContextBuilder(
				TranscriptReader transcriptReader,
				MeetingContextReader meetingContextReader,
				RagContextReader ragContextReader
		) {
			return new SummaryContextBuilder(transcriptReader, meetingContextReader, ragContextReader);
		}

		@Bean
		BlockingSummaryAiClient blockingSummaryAiClient() {
			return new BlockingSummaryAiClient();
		}

		@Bean
		SummaryAiClient summaryAiClient(BlockingSummaryAiClient blockingSummaryAiClient) {
			return blockingSummaryAiClient;
		}

		@Bean
		SummaryJobProcessor summaryJobProcessor(
				SummaryJobStore jobStore,
				MeetingSummaryStore summaryStore,
				SummaryContextBuilder contextBuilder,
				SummaryAiClient summaryAiClient,
				org.springframework.context.ApplicationEventPublisher eventPublisher
		) {
			return new SummaryJobProcessor(jobStore, summaryStore, contextBuilder, summaryAiClient, eventPublisher);
		}

		@Bean
		MeetingSummaryService meetingSummaryService(
				SummaryJobStore jobStore,
				MeetingSummaryStore summaryStore,
				SummaryJobProcessor processor
		) {
			// 이 테스트는 비동기/중복요청 동작을 검증하므로 회의는 항상 종료된 것으로 간주한다.
			return new MeetingSummaryService(jobStore, summaryStore, processor, meetingId -> true);
		}
	}

	static class RecordingSummaryJobStore extends InMemorySummaryJobStore {

		private SummaryJob lastSavedJob;

		@Override
		public SummaryJob save(SummaryJob job) {
			lastSavedJob = super.save(job);
			return lastSavedJob;
		}

		SummaryJob lastSavedJob() {
			return lastSavedJob;
		}
	}

	static class BlockingSummaryAiClient implements SummaryAiClient {

		private final CountDownLatch started = new CountDownLatch(1);
		private final CountDownLatch release = new CountDownLatch(1);

		@Override
		public GeneratedSummary generate(SummaryContext context) {
			started.countDown();
			try {
				release.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("요약 작업이 중단되었습니다.", e);
			}
			return new GeneratedSummary("요약", List.of(), List.of(), List.of(), List.of());
		}

		boolean awaitStarted() throws InterruptedException {
			return started.await(1, TimeUnit.SECONDS);
		}

		void release() {
			release.countDown();
		}
	}
}
