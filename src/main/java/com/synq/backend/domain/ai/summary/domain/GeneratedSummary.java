package com.synq.backend.domain.ai.summary.domain;

import java.util.List;

public record GeneratedSummary(
		String overallSummary,
		List<String> keyTopics,
		List<String> decisions,
		List<String> actionItems,
		List<String> openQuestions
) {
	public GeneratedSummary {
		// API 응답과 저장 결과가 외부 리스트 변경의 영향을 받지 않게 한다.
		keyTopics = immutableListOrEmpty(keyTopics);
		decisions = immutableListOrEmpty(decisions);
		actionItems = immutableListOrEmpty(actionItems);
		openQuestions = immutableListOrEmpty(openQuestions);
	}

	private static <T> List<T> immutableListOrEmpty(List<T> values) {
		return values == null ? List.of() : List.copyOf(values);
	}
}
