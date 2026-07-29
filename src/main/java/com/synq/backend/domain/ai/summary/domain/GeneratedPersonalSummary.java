package com.synq.backend.domain.ai.summary.domain;

import java.util.List;

public record GeneratedPersonalSummary(
		String personalSummary,
		List<String> keyPoints,
		List<String> myActionItems,
		List<String> followUpQuestions
) {
	public GeneratedPersonalSummary {
		keyPoints = immutableListOrEmpty(keyPoints);
		myActionItems = immutableListOrEmpty(myActionItems);
		followUpQuestions = immutableListOrEmpty(followUpQuestions);
	}

	private static <T> List<T> immutableListOrEmpty(List<T> values) {
		return values == null ? List.of() : List.copyOf(values);
	}
}
