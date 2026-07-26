package com.synq.backend.domain.ai.assistant.domain;

import java.util.List;

public record AiChatResult(
		String answer,
		List<AiChatSource> sources,
		List<String> suggestedQuestions
) {
	public AiChatResult {
		sources = List.copyOf(sources);
		suggestedQuestions = List.copyOf(suggestedQuestions);
	}
}
