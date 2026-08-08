package com.synq.backend.domain.ai.summary.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.synq.backend.domain.ai.summary.domain.GeneratedPersonalSummary;
import com.synq.backend.domain.ai.summary.domain.GeneratedSummary;
import com.synq.backend.domain.ai.summary.domain.DiscussionSection;
import com.synq.backend.domain.ai.summary.domain.MeetingSummary;
import com.synq.backend.domain.ai.summary.domain.PersonalSummaryTarget;
import com.synq.backend.domain.ai.summary.domain.SummaryJob;
import com.synq.backend.domain.ai.summary.domain.SummaryJobStatus;
import com.synq.backend.domain.ai.summary.application.SummaryResultWriter;
import com.synq.backend.domain.ai.summary.infrastructure.persistence.JpaMeetingSummaryStore;
import com.synq.backend.domain.ai.summary.infrastructure.persistence.JpaPersonalSummaryStore;
import com.synq.backend.domain.ai.summary.infrastructure.persistence.JpaSummaryJobStore;
import com.synq.backend.support.PostgresTestContainer;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class SummaryPersistenceStoreTest extends PostgresTestContainer {

	private static final long MEETING_ID = 990_001L;
	private static final long USER_ID = 990_001L;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private JpaSummaryJobStore jobStore;

	@Autowired
	private JpaMeetingSummaryStore meetingSummaryStore;

	@Autowired
	private JpaPersonalSummaryStore personalSummaryStore;

	@Autowired
	private SummaryResultWriter resultWriter;

	@BeforeEach
	void setUpReferences() {
		jdbcTemplate.update("""
				INSERT INTO users (user_id, name, provider)
				VALUES (?, '요약 테스트', 'LOCAL')
				ON CONFLICT (user_id) DO NOTHING
				""", USER_ID);
		jdbcTemplate.update("""
				INSERT INTO meeting (id, project_id, title, status)
				VALUES (?, 1, '요약 저장 테스트', 'SUMMARIZING')
				ON CONFLICT (id) DO NOTHING
				""", MEETING_ID);
	}

	@Test
	void Job과_전체_개인_요약을_같은_버전으로_저장하고_조회한다() {
		SummaryJob job = jobStore.save(SummaryJob.queued(MEETING_ID, "test-model", "test-v1"));
		SummaryJob processingJob = jobStore.startIfQueued(job.id()).orElseThrow();
		assertThat(resultWriter.saveIfJobProcessing(
				processingJob,
				new GeneratedSummary(
						"서비스 온보딩 개선 논의",
						"한 줄 요약",
						List.of("주제"),
						List.of(new DiscussionSection("주요 논의", List.of("세부 논의"))),
						List.of(),
						List.of(),
						List.of("확인 항목")
				),
				List.of(new SummaryResultWriter.PersonalGeneration(
						new PersonalSummaryTarget(USER_ID, "DEV_TECH", List.of("TECH_RISK")),
						new GeneratedPersonalSummary("개인 요약", List.of("핵심"), List.of(), List.of())
				)),
				0
		)).isTrue();

		MeetingSummary overall = meetingSummaryStore.findLatestByMeetingId(MEETING_ID).orElseThrow();
		var personal = personalSummaryStore.findLatestByMeetingIdAndUserId(MEETING_ID, USER_ID).orElseThrow();
		assertThat(jobStore.findById(job.id()).orElseThrow().status()).isEqualTo(SummaryJobStatus.COMPLETED);
		assertThat(overall.version()).isEqualTo(1);
		assertThat(overall.content().oneLineSummary()).isEqualTo("한 줄 요약");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT title FROM meeting WHERE id = ?", String.class, MEETING_ID))
				.isEqualTo("서비스 온보딩 개선 논의");
		assertThat(overall.content().discussionSections())
				.containsExactly(new DiscussionSection("주요 논의", List.of("세부 논의")));
		assertThat(personal.version()).isEqualTo(overall.version());
		assertThat(personal.content().personalSummary()).isEqualTo("개인 요약");
	}

	@Test
	void 실패로_전환된_Job은_지연_실행되더라도_요약을_저장하지_않는다() {
		SummaryJob job = jobStore.save(SummaryJob.queued(MEETING_ID, "test-model", "test-v1"));
		SummaryJob processingJob = jobStore.startIfQueued(job.id()).orElseThrow();
		assertThat(jobStore.failIfActive(job.id(), "작업 만료")).isTrue();

		assertThat(resultWriter.saveIfJobProcessing(
				processingJob,
				new GeneratedSummary("한 줄 요약", List.of("주제"), List.of(), List.of(), List.of(), List.of("확인 항목")),
				List.of(new SummaryResultWriter.PersonalGeneration(
						new PersonalSummaryTarget(USER_ID, "DEV_TECH", List.of("TECH_RISK")),
						new GeneratedPersonalSummary("개인 요약", List.of("핵심"), List.of(), List.of())
				)),
				0
		)).isFalse();

		assertThat(jobStore.findById(job.id()).orElseThrow().status()).isEqualTo(SummaryJobStatus.FAILED);
		assertThat(meetingSummaryStore.findLatestByMeetingId(MEETING_ID)).isEmpty();
		assertThat(personalSummaryStore.findLatestByMeetingIdAndUserId(MEETING_ID, USER_ID)).isEmpty();
	}

	@Test
	void 개인_요약이_일부_실패하면_부분_완료_상태와_실패_건수를_저장한다() {
		SummaryJob job = jobStore.save(SummaryJob.queued(MEETING_ID, "test-model", "test-v1"));
		SummaryJob processingJob = jobStore.startIfQueued(job.id()).orElseThrow();

		assertThat(resultWriter.saveIfJobProcessing(
				processingJob,
				new GeneratedSummary("한 줄 요약", List.of("주제"), List.of(), List.of(), List.of(), List.of()),
				List.of(),
				2
		)).isTrue();

		SummaryJob completedJob = jobStore.findById(job.id()).orElseThrow();
		assertThat(completedJob.status()).isEqualTo(SummaryJobStatus.COMPLETED_WITH_ERRORS);
		assertThat(completedJob.failedPersonalSummaryCount()).isEqualTo(2);
		assertThat(meetingSummaryStore.findLatestByMeetingId(MEETING_ID)).isPresent();
	}

	@Test
	void 개인_요약_실패_건수가_음수면_저장소가_거부한다() {
		SummaryJob job = jobStore.save(SummaryJob.queued(MEETING_ID, "test-model", "test-v1"));

		assertThatThrownBy(() -> jobStore.completeIfProcessing(job.id(), -1))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 신규_컬럼이_없는_기존_요약도_새_응답_구조로_조회한다() {
		UUID jobId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO ai_summary_job (id, meeting_id, status, retry_count, prompt_version, created_at)
				VALUES (?, ?, 'COMPLETED', 0, 'legacy', now())
				""", jobId, MEETING_ID);
		jdbcTemplate.update("""
				INSERT INTO meeting_summary (
					meeting_id, job_id, version, overall_summary, key_topics, decisions,
					action_items, open_questions, generated_at
				) VALUES (?, ?, 99, '기존 전체 요약', '["주제"]'::jsonb, '[]'::jsonb,
					'["확인 항목"]'::jsonb, '["논의 방향"]'::jsonb, now())
				""", MEETING_ID, jobId);

		MeetingSummary summary = meetingSummaryStore.findLatestByMeetingId(MEETING_ID).orElseThrow();

		assertThat(summary.content().oneLineSummary()).isEqualTo("기존 전체 요약");
		assertThat(summary.content().tentativeDirections()).containsExactly("논의 방향");
		assertThat(summary.content().confirmationItems()).containsExactly("확인 항목");
		assertThat(summary.content().discussionSections()).isEmpty();
	}
}
