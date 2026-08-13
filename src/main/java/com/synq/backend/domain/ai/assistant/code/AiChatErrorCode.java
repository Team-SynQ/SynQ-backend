package com.synq.backend.domain.ai.assistant.code;

import com.synq.backend.global.apipayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AiChatErrorCode implements BaseCode {
	NOT_PROJECT_MEMBER(
			HttpStatus.FORBIDDEN,
			"AI_CHAT403_1",
			"프로젝트 멤버만 AI 채팅을 이용할 수 있습니다."
	),
	CHAT_NOT_AVAILABLE(
			HttpStatus.CONFLICT,
			"AI_CHAT409_1",
			"진행 중인 회의에서만 새 AI 질문을 보낼 수 있습니다."
	),
	IDEMPOTENCY_KEY_REUSED(
			HttpStatus.CONFLICT,
			"AI_CHAT409_2",
			"같은 clientRequestId가 다른 요청에 사용되었습니다."
	),
	LINKED_SEGMENT_NOT_AVAILABLE(
			HttpStatus.UNPROCESSABLE_ENTITY,
			"AI_CHAT422_1",
			"선택 발화 연동은 전사 세그먼트 검증 기능이 준비된 후 사용할 수 있습니다."
	),
	AI_GENERATION_FAILED(
			HttpStatus.BAD_GATEWAY,
			"AI_CHAT502_1",
			"AI 답변을 생성하지 못했습니다."
	);

	private final HttpStatus status;
	private final String code;
	private final String message;
}
