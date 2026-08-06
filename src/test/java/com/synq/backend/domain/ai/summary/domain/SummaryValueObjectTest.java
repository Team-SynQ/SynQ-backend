package com.synq.backend.domain.ai.summary.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SummaryValueObjectTest {

	@Test
	void null_리스트는_빈_불변_리스트로_정규화한다() {
		GeneratedSummary summary = new GeneratedSummary("요약", null, null, null, null, null);
		SummaryContext context = new SummaryContext(1L, "전사", null);

		assertThat(summary.keyTopics()).isEmpty();
		assertThat(summary.decisions()).isEmpty();
		assertThat(summary.discussionSections()).isEmpty();
		assertThat(summary.tentativeDirections()).isEmpty();
		assertThat(summary.confirmationItems()).isEmpty();
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

	@Test
	void 개인_요약_실패_건수가_있으면_부분_완료로_전이한다() {
		SummaryJob partiallyCompleted = SummaryJob.queued(1L).start().complete(2);

		assertThat(partiallyCompleted.status()).isEqualTo(SummaryJobStatus.COMPLETED_WITH_ERRORS);
		assertThat(partiallyCompleted.failedPersonalSummaryCount()).isEqualTo(2);
		assertThat(partiallyCompleted.isStale(java.time.Instant.now(), java.time.Duration.ofMinutes(1))).isFalse();
	}

	@Test
	void 개인_요약_실패_건수는_음수일_수_없다() {
		assertThatThrownBy(() -> new SummaryJob(
				java.util.UUID.randomUUID(), 1L, SummaryJobStatus.QUEUED, 0, -1,
				"model", "v1", null, java.time.Instant.now(), null, null
		)).isInstanceOf(IllegalArgumentException.class);
	}
}
