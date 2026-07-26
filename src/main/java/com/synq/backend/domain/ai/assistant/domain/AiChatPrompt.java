package com.synq.backend.domain.ai.assistant.domain;

public record AiChatPrompt(
		Long meetingId,
		Long userId,
		String question,
		Long linkedSegmentId
) {
}
