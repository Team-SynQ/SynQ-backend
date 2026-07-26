package com.synq.backend.domain.ai.assistant.application;

import com.synq.backend.domain.ai.assistant.domain.AiChatMessage;

public record AiChatSendResult(
		AiChatMessage message,
		boolean created
) {
}
