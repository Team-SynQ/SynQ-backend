package com.synq.backend.domain.ai.context.domain;

import java.util.List;

/**
 * AI 호출에 필요한 기존 Live Context의 읽기 전용 형태다.
 */
public record LiveContextSnapshot(
        String rollingSummary,
        String currentTopic,
        List<String> decisions,
        List<String> actionItems,
        List<String> openQuestions
) {

    public LiveContextSnapshot {
        rollingSummary = rollingSummary == null ? "" : rollingSummary;
        decisions = decisions == null ? List.of() : List.copyOf(decisions);
        actionItems = actionItems == null ? List.of() : List.copyOf(actionItems);
        openQuestions = openQuestions == null ? List.of() : List.copyOf(openQuestions);
    }

    public static LiveContextSnapshot empty() {
        return new LiveContextSnapshot("", null, List.of(), List.of(), List.of());
    }

    public static LiveContextSnapshot from(LiveContext context) {
        return new LiveContextSnapshot(
                context.getRollingSummary(),
                context.getCurrentTopic(),
                context.getDecisions(),
                context.getActionItems(),
                context.getOpenQuestions()
        );
    }
}
