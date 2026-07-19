package com.synq.backend.domain.ai.summary.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SummaryValueObjectTest {

	@Test
	void null_리스트는_빈_불변_리스트로_정규화한다() {
		GeneratedSummary summary = new GeneratedSummary("요약", null, null, null, null);
		SummaryContext context = new SummaryContext(1L, "전사", "맥락", null);

		assertThat(summary.keyTopics()).isEmpty();
		assertThat(summary.decisions()).isEmpty();
		assertThat(summary.actionItems()).isEmpty();
		assertThat(summary.openQuestions()).isEmpty();
		assertThat(context.referenceContexts()).isEmpty();
	}
}
