package com.synq.backend.domain.ai.summary.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.synq.backend.domain.ai.event.SummaryFailedEvent;
import com.synq.backend.domain.ai.summary.mock.FakeSummaryAiClient;
import com.synq.backend.domain.ai.summary.mock.InMemoryMeetingSummaryStore;
import com.synq.backend.domain.ai.summary.mock.InMemoryPersonalSummaryStore;
import com.synq.backend.domain.ai.summary.mock.InMemorySummaryJobStore;
import com.synq.backend.domain.ai.summary.mock.MockRagContextReader;
import com.synq.backend.domain.ai.summary.mock.MockTranscriptReader;
import com.synq.backend.domain.ai.summary.domain.DiscussionSection;
import com.synq.backend.domain.ai.summary.domain.GeneratedSummary;
import com.synq.backend.domain.ai.summary.domain.SummaryJob;
import com.synq.backend.domain.ai.summary.domain.SummaryJobStatus;
import com.synq.backend.domain.ai.summary.domain.SummaryAiClient;
import com.synq.backend.domain.ai.summary.domain.TranscriptSegment;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

class SummaryJobProcessorTest {

	@Test
	void 요약_작업을_완료하고_결과를_저장한다() {
		var jobStore = new InMemorySummaryJobStore();
		var summaryStore = new InMemoryMeetingSummaryStore();
		var contextBuilder = new SummaryContextBuilder(
				new MockTranscriptReader(), new MockRagContextReader(), testProperties());
		var processor = processor(jobStore, summaryStore, contextBuilder, new FakeSummaryAiClient(), event -> {});
		SummaryJob job = jobStore.save(SummaryJob.queued(1L));

		processor.process(job.id());

		assertThat(jobStore.findById(job.id()).orElseThrow().status()).isEqualTo(SummaryJobStatus.COMPLETED);
		assertThat(summaryStore.findLatestByMeetingId(1L).orElseThrow().content().confirmationItems())
				.contains("API 명세 초안을 작성한다.");
	}

	@Test
	void 예외_메시지가_없어도_안전한_실패_원인을_저장한다() {
		var jobStore = new InMemorySummaryJobStore();
		var summaryStore = new InMemoryMeetingSummaryStore();
		var contextBuilder = new SummaryContextBuilder(
				new MockTranscriptReader(), new MockRagContextReader(), testProperties());
		var processor = processor(
				jobStore, summaryStore, contextBuilder, context -> {
					throw new IllegalStateException();
				}, event -> {});
		SummaryJob job = jobStore.save(SummaryJob.queued(1L));

		processor.process(job.id());

		SummaryJob failedJob = jobStore.findById(job.id()).orElseThrow();
		assertThat(failedJob.status()).isEqualTo(SummaryJobStatus.FAILED);
		assertThat(failedJob.errorMessage()).isEqualTo(SummaryJobProcessor.SUMMARY_GENERATION_FAILED_MESSAGE);
	}

	@Test
	void 상세_예외는_Job에만_저장하고_SSE에는_안전한_메시지를_전달한다() {
		var jobStore = new InMemorySummaryJobStore();
		var summaryStore = new InMemoryMeetingSummaryStore();
		var contextBuilder = new SummaryContextBuilder(
				new MockTranscriptReader(), new MockRagContextReader(), testProperties());
		ApplicationEventPublisher eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
		var processor = processor(
				jobStore,
				summaryStore,
				contextBuilder,
				context -> {
					throw new IllegalStateException("OpenAI 내부 응답: 민감한 상세 내용");
				},
				eventPublisher
		);
		SummaryJob job = jobStore.save(SummaryJob.queued(1L));

		processor.process(job.id());

		assertThat(jobStore.findById(job.id()).orElseThrow().errorMessage())
				.isEqualTo(SummaryJobProcessor.SUMMARY_GENERATION_FAILED_MESSAGE);
		ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
		verify(eventPublisher).publishEvent((Object) eventCaptor.capture());
		SummaryFailedEvent event = (SummaryFailedEvent) eventCaptor.getValue();
		assertThat(event.reason()).isEqualTo(SummaryJobProcessor.SUMMARY_GENERATION_FAILED_MESSAGE);
	}

	@Test
	void 긴_전사에서_부분_요약이_길어져도_중간_컨텍스트를_축약한다() {
		var jobStore = new InMemorySummaryJobStore();
		var summaryStore = new InMemoryMeetingSummaryStore();
		var properties = new SummaryProperties("test-model", "test-v1", 50);
		var contextBuilder = new SummaryContextBuilder(
				meetingId -> List.of(new TranscriptSegment(null, "가".repeat(200))),
				new MockRagContextReader(),
				properties
		);
		SummaryAiClient expandingClient = context -> new GeneratedSummary(
				"요약".repeat(100),
				List.of("주제".repeat(100)),
				List.of(new DiscussionSection("논의".repeat(100), List.of("세부 내용".repeat(100)))),
				List.of(),
				List.of(),
				List.of()
		);
		var processor = processor(jobStore, summaryStore, contextBuilder, expandingClient, event -> {}, properties);
		SummaryJob job = jobStore.save(SummaryJob.queued(1L));

		processor.process(job.id());

		assertThat(jobStore.findById(job.id()).orElseThrow().status()).isEqualTo(SummaryJobStatus.COMPLETED);
		assertThat(summaryStore.findLatestByMeetingId(1L)).isPresent();
	}

	private SummaryProperties testProperties() {
		return new SummaryProperties("test-model", "test-v1", 600_000);
	}

	private SummaryJobProcessor processor(
			InMemorySummaryJobStore jobStore,
			InMemoryMeetingSummaryStore summaryStore,
			SummaryContextBuilder contextBuilder,
			SummaryAiClient summaryAiClient,
			ApplicationEventPublisher eventPublisher
	) {
		return processor(jobStore, summaryStore, contextBuilder, summaryAiClient, eventPublisher, testProperties());
	}

	private SummaryJobProcessor processor(
			InMemorySummaryJobStore jobStore,
			InMemoryMeetingSummaryStore summaryStore,
			SummaryContextBuilder contextBuilder,
			SummaryAiClient summaryAiClient,
			ApplicationEventPublisher eventPublisher,
			SummaryProperties properties
	) {
		return new SummaryJobProcessor(
				jobStore,
				contextBuilder,
				summaryAiClient,
				new FakeSummaryAiClient(),
				meetingId -> java.util.List.of(),
				new SummaryResultWriter(summaryStore, new InMemoryPersonalSummaryStore(), jobStore),
				properties,
				eventPublisher
		);
	}
}
