package com.synq.backend.domain.ai.assistant.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AiChatSendRequest(
		@Schema(
				description = "AI에게 보낼 질문. 공백만 입력할 수 없으며 최대 2,000자입니다.",
				example = "현재 논의된 배포 일정의 위험 요소를 정리해 줘",
				minLength = 1,
				maxLength = 2000,
				requiredMode = Schema.RequiredMode.REQUIRED
		)
		@NotBlank
		@Size(min = 1, max = 2000)
		String question,

		@Schema(description = "질문과 연결할 특정 전사 세그먼트 ID. 회의 전체 맥락에 질문하는 경우 생략합니다.", nullable = true, example = "25")
		@Positive
		Long linkedSegmentId,

		@Schema(
				description = "중복 요청 방지를 위한 멱등성 키. 재시도할 때는 같은 UUID를 사용해야 합니다.",
				example = "550e8400-e29b-41d4-a716-446655440000",
				requiredMode = Schema.RequiredMode.REQUIRED
		)
		@NotNull
		UUID clientRequestId
) {
}
