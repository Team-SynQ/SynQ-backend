package com.synq.backend.domain.ai.summary.domain;

import java.util.List;

public record GeneratedSummary(
		String title,
		String oneLineSummary,
		List<String> keyTopics,
		List<DiscussionSection> discussionSections,
		List<String> decisions,
		List<String> tentativeDirections,
		List<String> confirmationItems
) {
	public GeneratedSummary {
		title = title == null ? "" : title.strip();
		keyTopics = immutableListOrEmpty(keyTopics);
		discussionSections = immutableListOrEmpty(discussionSections);
		decisions = immutableListOrEmpty(decisions);
		tentativeDirections = immutableListOrEmpty(tentativeDirections);
		confirmationItems = immutableListOrEmpty(confirmationItems);
	}

	/** 기존 호출부가 제목 없이 생성해도 동작하도록 기본 제목을 둔다. */
	public GeneratedSummary(
			String oneLineSummary,
			List<String> keyTopics,
			List<DiscussionSection> discussionSections,
			List<String> decisions,
			List<String> tentativeDirections,
			List<String> confirmationItems
	) {
		this("", oneLineSummary, keyTopics, discussionSections, decisions, tentativeDirections, confirmationItems);
	}

	private static <T> List<T> immutableListOrEmpty(List<T> values) {
		return values == null ? List.of() : List.copyOf(values);
	}
}
