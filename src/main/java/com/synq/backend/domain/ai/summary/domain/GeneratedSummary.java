package com.synq.backend.domain.ai.summary.domain;

import java.util.List;

public record GeneratedSummary(
		String oneLineSummary,
		List<String> keyTopics,
		List<DiscussionSection> discussionSections,
		List<String> decisions,
		List<String> tentativeDirections,
		List<String> confirmationItems
) {
	public GeneratedSummary {
		keyTopics = immutableListOrEmpty(keyTopics);
		discussionSections = immutableListOrEmpty(discussionSections);
		decisions = immutableListOrEmpty(decisions);
		tentativeDirections = immutableListOrEmpty(tentativeDirections);
		confirmationItems = immutableListOrEmpty(confirmationItems);
	}

	private static <T> List<T> immutableListOrEmpty(List<T> values) {
		return values == null ? List.of() : List.copyOf(values);
	}
}
