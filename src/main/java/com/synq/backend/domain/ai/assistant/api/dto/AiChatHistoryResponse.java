package com.synq.backend.domain.ai.assistant.api.dto;

import com.synq.backend.domain.ai.assistant.domain.AiChatMessage;
import java.util.List;
import org.springframework.data.domain.Page;

public record AiChatHistoryResponse(
		List<AiChatMessageResponse> messages,
		int page,
		int size,
		boolean hasNext
) {
	public static AiChatHistoryResponse from(Page<AiChatMessage> history) {
		return new AiChatHistoryResponse(
				history.getContent().stream().map(AiChatMessageResponse::from).toList(),
				history.getNumber(),
				history.getSize(),
				history.hasNext()
		);
	}
}
