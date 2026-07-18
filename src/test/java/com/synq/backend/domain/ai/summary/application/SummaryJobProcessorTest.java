package com.synq.backend.domain.ai.summary.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.synq.backend.domain.ai.summary.mock.FakeSummaryAiClient;
import com.synq.backend.domain.ai.summary.mock.InMemoryMeetingSummaryStore;
import com.synq.backend.domain.ai.summary.mock.InMemorySummaryJobStore;
import com.synq.backend.domain.ai.summary.mock.MockMeetingContextReader;
import com.synq.backend.domain.ai.summary.mock.MockRagContextReader;
import com.synq.backend.domain.ai.summary.mock.MockTranscriptReader;
import com.synq.backend.domain.ai.summary.domain.SummaryJob;
import com.synq.backend.domain.ai.summary.domain.SummaryJobStatus;
import org.junit.jupiter.api.Test;

class SummaryJobProcessorTest {

	@Test
	void 요약_작업을_완료하고_결과를_저장한다() {
		var jobStore = new InMemorySummaryJobStore();
		var summaryStore = new InMemoryMeetingSummaryStore();
		var contextBuilder = new SummaryContextBuilder(
				new MockTranscriptReader(), new MockMeetingContextReader(), new MockRagContextReader());
		var processor = new SummaryJobProcessor(jobStore, summaryStore, contextBuilder, new FakeSummaryAiClient());
		SummaryJob job = jobStore.save(SummaryJob.queued(1L));

		processor.process(job.id());

		assertThat(jobStore.findById(job.id()).orElseThrow().status()).isEqualTo(SummaryJobStatus.COMPLETED);
		assertThat(summaryStore.findLatestByMeetingId(1L).orElseThrow().content().actionItems())
				.contains("API 명세 초안을 작성한다.");
	}

	@Test
	void 예외_메시지가_없어도_실패_상태와_원인을_저장한다() {
		var jobStore = new InMemorySummaryJobStore();
		var summaryStore = new InMemoryMeetingSummaryStore();
		var contextBuilder = new SummaryContextBuilder(
				new MockTranscriptReader(), new MockMeetingContextReader(), new MockRagContextReader());
		var processor = new SummaryJobProcessor(
				jobStore, summaryStore, contextBuilder, context -> {
					throw new IllegalStateException();
				});
		SummaryJob job = jobStore.save(SummaryJob.queued(1L));

		processor.process(job.id());

		SummaryJob failedJob = jobStore.findById(job.id()).orElseThrow();
		assertThat(failedJob.status()).isEqualTo(SummaryJobStatus.FAILED);
		assertThat(failedJob.errorMessage()).isEqualTo("IllegalStateException");
	}
}
