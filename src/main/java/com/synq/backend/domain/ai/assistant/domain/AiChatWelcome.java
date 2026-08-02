package com.synq.backend.domain.ai.assistant.domain;

import java.util.List;

/**
 * AI Chat 패널을 처음 열었을 때 표시하는 안내와 추천 질문이다.
 */
public record AiChatWelcome(String welcomeMessage, List<String> suggestedQuestions) {
	public AiChatWelcome {
		suggestedQuestions = List.copyOf(suggestedQuestions);
	}
}
