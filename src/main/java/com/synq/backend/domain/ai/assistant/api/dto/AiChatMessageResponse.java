package com.synq.backend.domain.ai.assistant.api.dto;

import com.synq.backend.domain.ai.assistant.domain.AiChatMessage;
import com.synq.backend.domain.ai.assistant.domain.AiChatSource;
import com.synq.backend.domain.ai.assistant.domain.AiChatStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AiChatMessageResponse(
		Long id,
		Long meetingId,
		UUID clientRequestId,
		Long linkedSegmentId,
		String question,
		String answer,
		AiChatStatus status,
		List<AiChatSource> sources,
		List<String> suggestedQuestions,
		String errorCode,
		String errorMessage,
		LocalDateTime createdAt
) {
	public static AiChatMessageResponse from(AiChatMessage message) {
		return new AiChatMessageResponse(
				message.getId(),
				message.getMeetingId(),
				message.getClientRequestId(),
				message.getLinkedSegmentId(),
				message.getQuestion(),
				message.getAnswer(),
				message.getStatus(),
				List.copyOf(message.getSourceRefs()),
				List.copyOf(message.getSuggestedQuestions()),
				message.getErrorCode(),
				message.getErrorMessage(),
				message.getCreatedAt()
		);
	}
}
