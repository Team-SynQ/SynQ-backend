package com.synq.backend.domain.ai.assistant.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AiChatSendRequest(
		@NotBlank
		@Size(max = 2000)
		String question,

		@Positive
		Long linkedSegmentId,

		@NotNull
		UUID clientRequestId
) {
}
