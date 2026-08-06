package com.synq.backend.domain.ai.summary.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.synq.backend.domain.ai.summary.domain.SummaryJob;
import org.junit.jupiter.api.Test;

class SummaryJobResponseTest {

	@Test
	void 부분_완료_Job의_개인_요약_실패_건수를_응답에_포함한다() {
		SummaryJob job = SummaryJob.queued(1L).start().complete(2);

		SummaryJobResponse response = SummaryJobResponse.from(job);

		assertThat(response.status()).isEqualTo("COMPLETED_WITH_ERRORS");
		assertThat(response.failedPersonalSummaryCount()).isEqualTo(2);
	}
}
