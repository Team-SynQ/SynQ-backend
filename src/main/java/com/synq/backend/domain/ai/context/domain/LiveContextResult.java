package com.synq.backend.domain.ai.context.domain;

import java.util.List;

/**
 * AI가 새 전사를 반영해 반환한 회의 맥락이다.
 */
public record LiveContextResult(
        String rollingSummary,
        String currentTopic,
        List<String> decisions,
        List<String> actionItems,
        List<String> openQuestions,
        AutoHintDecision autoHintDecision
) {

	public LiveContextResult(
			String rollingSummary,
			String currentTopic,
			List<String> decisions,
			List<String> actionItems,
			List<String> openQuestions
	) {
		this(rollingSummary, currentTopic, decisions, actionItems, openQuestions, AutoHintDecision.none());
	}

    public LiveContextResult {
        rollingSummary = rollingSummary == null ? "" : rollingSummary;
        decisions = decisions == null ? List.of() : List.copyOf(decisions);
        actionItems = actionItems == null ? List.of() : List.copyOf(actionItems);
        openQuestions = openQuestions == null ? List.of() : List.copyOf(openQuestions);
        autoHintDecision = autoHintDecision == null ? AutoHintDecision.none() : autoHintDecision;
    }
}
