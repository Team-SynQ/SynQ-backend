package com.synq.backend.domain.ai.assistant.domain;

public record AiChatPrompt(
		String question,
		AiChatContext context
) {
}
