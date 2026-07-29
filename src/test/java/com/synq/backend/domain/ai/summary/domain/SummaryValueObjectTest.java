package com.synq.backend.domain.ai.summary.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SummaryValueObjectTest {

	@Test
	void null_리스트는_빈_불변_리스트로_정규화한다() {
		GeneratedSummary summary = new GeneratedSummary("요약", null, null, null, null);
		SummaryContext context = new SummaryContext(1L, "전사", null);

		assertThat(summary.keyTopics()).isEmpty();
		assertThat(summary.decisions()).isEmpty();
		assertThat(summary.actionItems()).isEmpty();
		assertThat(summary.openQuestions()).isEmpty();
		assertThat(context.referenceContexts()).isEmpty();
	}

	@Test
	void 요약_작업은_허용된_상태_순서로만_전이한다() {
		SummaryJob queued = SummaryJob.queued(1L);

		assertThatThrownBy(queued::complete).isInstanceOf(IllegalStateException.class);
		SummaryJob processing = queued.start();
		assertThatThrownBy(processing::start).isInstanceOf(IllegalStateException.class);

		SummaryJob completed = processing.complete();
		assertThatThrownBy(() -> completed.fail("실패")).isInstanceOf(IllegalStateException.class);
	}
}
