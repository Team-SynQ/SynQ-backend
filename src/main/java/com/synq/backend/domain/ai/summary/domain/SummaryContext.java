package com.synq.backend.domain.ai.summary.domain;

import java.util.List;

public record SummaryContext(
		Long meetingId,
		String transcript,
		String rollingSummary,
		List<String> referenceContexts
) {
	public SummaryContext {
		// Reader 구현체가 반환한 컬렉션이 이후 변경돼도 AI 입력은 변하지 않게 고정한다.
		referenceContexts = referenceContexts == null ? List.of() : List.copyOf(referenceContexts);
	}
}
