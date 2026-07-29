package com.synq.backend.domain.ai.summary.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.synq.backend.domain.ai.summary.domain.GeneratedPersonalSummary;
import com.synq.backend.domain.ai.summary.domain.GeneratedSummary;
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
		SummaryJob processingJob = jobStore.save(job.start());
		MeetingSummary overall = resultWriter.save(
				processingJob,
				new GeneratedSummary("전체 요약", List.of("주제"), List.of(), List.of(), List.of()),
				List.of(new SummaryResultWriter.PersonalGeneration(
						new PersonalSummaryTarget(USER_ID, "DEV_TECH", List.of("TECH_RISK")),
						new GeneratedPersonalSummary("개인 요약", List.of("핵심"), List.of(), List.of())
				))
		);

		assertThat(jobStore.findById(job.id()).orElseThrow().status()).isEqualTo(SummaryJobStatus.COMPLETED);
		assertThat(meetingSummaryStore.findLatestByMeetingId(MEETING_ID).orElseThrow().version()).isEqualTo(1);
		assertThat(personalSummaryStore.findLatestByMeetingIdAndUserId(MEETING_ID, USER_ID)
				.orElseThrow().content().personalSummary()).isEqualTo("개인 요약");
	}
}
