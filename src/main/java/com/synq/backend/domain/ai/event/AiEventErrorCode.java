package com.synq.backend.domain.ai.event;

import com.synq.backend.global.apipayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AiEventErrorCode implements BaseCode {
	NOT_MEETING_PARTICIPANT(
			HttpStatus.FORBIDDEN,
			"AI_EVENT403_1",
			"회의 참여자만 AI 결과를 구독할 수 있습니다."
	);

	private final HttpStatus status;
	private final String code;
	private final String message;
}
