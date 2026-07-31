package com.synq.backend.domain.ai.assistant.api.dto;

import com.synq.backend.domain.ai.assistant.domain.AiChatWelcome;
import java.util.List;

public record AiChatWelcomeResponse(String welcomeMessage, List<String> suggestedQuestions) {
	public static AiChatWelcomeResponse from(AiChatWelcome welcome) {
		return new AiChatWelcomeResponse(welcome.welcomeMessage(), welcome.suggestedQuestions());
	}
}
