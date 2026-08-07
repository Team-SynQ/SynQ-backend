package com.synq.backend.domain.ai.summary.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.synq.backend.domain.ai.summary.domain.GeneratedPersonalSummary;
import com.synq.backend.domain.ai.summary.domain.PersonalSummaryTarget;
import com.synq.backend.domain.ai.summary.domain.PersonalSummaryAiClient;
import com.synq.backend.domain.ai.summary.domain.SummaryJob;
import com.synq.backend.domain.ai.summary.mock.FakeSummaryAiClient;
import com.synq.backend.domain.ai.summary.mock.InMemoryMeetingSummaryStore;
import com.synq.backend.domain.ai.summary.mock.InMemoryPersonalSummaryStore;
import com.synq.backend.domain.ai.summary.mock.InMemorySummaryJobStore;
import com.synq.backend.domain.ai.summary.mock.MockRagContextReader;
import com.synq.backend.domain.ai.summary.mock.MockTranscriptReader;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PersonalSummaryGenerationTest {

	@Test
	void 전체_요약과_같은_버전으로_참여자별_요약을_저장한다() {
		var jobStore = new InMemorySummaryJobStore();
		var meetingSummaryStore = new InMemoryMeetingSummaryStore();
		var personalSummaryStore = new InMemoryPersonalSummaryStore();
		var fakeClient = new FakeSummaryAiClient();
		var contextBuilder = new SummaryContextBuilder(
				new MockTranscriptReader(),
				new MockRagContextReader(),
				new SummaryProperties("test-model", "test-v1", 600_000)
		);
		var processor = new SummaryJobProcessor(
				jobStore,
				contextBuilder,
				fakeClient,
				fakeClient,
				meetingId -> List.of(new PersonalSummaryTarget(
						7L,
						"DEV_TECH - 백엔드",
						List.of("TECH_RISK", "ACTION_ITEM")
				)),
				new SummaryResultWriter(meetingSummaryStore, personalSummaryStore, jobStore),
				new SummaryProperties("test-model", "test-v1", 600_000),
				event -> {
				}
		);
		SummaryJob job = jobStore.save(SummaryJob.queued(1L));

		processor.process(job.id());

		var overall = meetingSummaryStore.findLatestByMeetingId(1L).orElseThrow();
		var personal = personalSummaryStore.findLatestByMeetingIdAndUserId(1L, 7L).orElseThrow();
		assertThat(personal.version()).isEqualTo(overall.version());
		assertThat(personal.role()).isEqualTo("DEV_TECH - 백엔드");
		assertThat(personal.content().personalSummary()).contains("DEV_TECH");
	}

	@Test
	void 입력이_제한보다_길면_부분_요약을_합쳐_최종_요약을_생성한다() {
		var jobStore = new InMemorySummaryJobStore();
		var meetingSummaryStore = new InMemoryMeetingSummaryStore();
		var personalSummaryStore = new InMemoryPersonalSummaryStore();
		var fakeClient = new FakeSummaryAiClient();
		com.synq.backend.domain.ai.summary.domain.SummaryAiClient compactSummaryClient =
				context -> new com.synq.backend.domain.ai.summary.domain.GeneratedSummary(
						"짧은 부분 요약", List.of(), List.of(), List.of(), List.of(), List.of());
		var contextBuilder = new SummaryContextBuilder(
				meetingId -> List.of(new com.synq.backend.domain.ai.summary.domain.TranscriptSegment(
						null,
						"가".repeat(300)
				)),
				new MockRagContextReader(),
				new SummaryProperties("test-model", "test-v1", 100)
		);
		var processor = new SummaryJobProcessor(
				jobStore,
				contextBuilder,
				compactSummaryClient,
				fakeClient,
				meetingId -> List.of(),
				new SummaryResultWriter(meetingSummaryStore, personalSummaryStore, jobStore),
				new SummaryProperties("test-model", "test-v1", 100),
				event -> {
				}
		);
		SummaryJob job = jobStore.save(SummaryJob.queued(1L));

		processor.process(job.id());

		assertThat(meetingSummaryStore.findLatestByMeetingId(1L)).isPresent();
		assertThat(jobStore.findById(job.id()).orElseThrow().status().name()).isEqualTo("COMPLETED");
	}

	@Test
	void 일부_개인_요약_생성에_실패해도_전체_요약과_성공한_개인_요약은_저장한다() {
		var jobStore = new InMemorySummaryJobStore();
		var meetingSummaryStore = new InMemoryMeetingSummaryStore();
		var personalSummaryStore = new InMemoryPersonalSummaryStore();
		var fakeClient = new FakeSummaryAiClient();
		var failedUserAttempts = new AtomicInteger();
		PersonalSummaryAiClient personalSummaryAiClient = (context, overall, target) -> {
			if (target.userId().equals(8L)) {
				failedUserAttempts.incrementAndGet();
				throw new IllegalStateException("개인 요약 생성 실패");
			}
			return new GeneratedPersonalSummary("성공한 개인 요약", List.of(), List.of(), List.of());
		};
		var contextBuilder = new SummaryContextBuilder(
				new MockTranscriptReader(),
				new MockRagContextReader(),
				new SummaryProperties("test-model", "test-v1", 600_000)
		);
		var processor = new SummaryJobProcessor(
				jobStore,
				contextBuilder,
				fakeClient,
				personalSummaryAiClient,
				meetingId -> List.of(
						new PersonalSummaryTarget(7L, "DEV_TECH", List.of()),
						new PersonalSummaryTarget(8L, "DESIGN", List.of())
				),
				new SummaryResultWriter(meetingSummaryStore, personalSummaryStore, jobStore),
				new SummaryProperties("test-model", "test-v1", 600_000),
				event -> {
				}
		);
		SummaryJob job = jobStore.save(SummaryJob.queued(1L));

		processor.process(job.id());

		var completedJob = jobStore.findById(job.id()).orElseThrow();
		assertThat(completedJob.status().name()).isEqualTo("COMPLETED_WITH_ERRORS");
		assertThat(completedJob.failedPersonalSummaryCount()).isEqualTo(1);
		assertThat(failedUserAttempts).hasValue(2);
		assertThat(meetingSummaryStore.findLatestByMeetingId(1L)).isPresent();
		assertThat(personalSummaryStore.findLatestByMeetingIdAndUserId(1L, 7L)).isPresent();
		assertThat(personalSummaryStore.findLatestByMeetingIdAndUserId(1L, 8L)).isEmpty();
	}

	@Test
	void 개인_요약이_재시도에서_성공하면_정상_완료로_처리한다() {
		var jobStore = new InMemorySummaryJobStore();
		var meetingSummaryStore = new InMemoryMeetingSummaryStore();
		var personalSummaryStore = new InMemoryPersonalSummaryStore();
		var fakeClient = new FakeSummaryAiClient();
		var attempts = new AtomicInteger();
		PersonalSummaryAiClient personalSummaryAiClient = (context, overall, target) -> {
			if (attempts.incrementAndGet() == 1) {
				throw new IllegalStateException("일시적인 제공자 오류");
			}
			return new GeneratedPersonalSummary("재시도 성공", List.of(), List.of(), List.of());
		};
		var processor = new SummaryJobProcessor(
				jobStore,
				new SummaryContextBuilder(new MockTranscriptReader(), new MockRagContextReader(),
						new SummaryProperties("test-model", "test-v1", 600_000)),
				fakeClient,
				personalSummaryAiClient,
				meetingId -> List.of(new PersonalSummaryTarget(7L, "DEV_TECH", List.of())),
				new SummaryResultWriter(meetingSummaryStore, personalSummaryStore, jobStore),
				new SummaryProperties("test-model", "test-v1", 600_000),
				event -> {
				}
		);
		SummaryJob job = jobStore.save(SummaryJob.queued(1L));

		processor.process(job.id());

		var completedJob = jobStore.findById(job.id()).orElseThrow();
		assertThat(completedJob.status().name()).isEqualTo("COMPLETED");
		assertThat(completedJob.failedPersonalSummaryCount()).isZero();
		assertThat(attempts).hasValue(2);
		assertThat(personalSummaryStore.findLatestByMeetingIdAndUserId(1L, 7L)).isPresent();
	}
}
