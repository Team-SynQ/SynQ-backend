package com.synq.backend.domain.ai.assistant.code;

import com.synq.backend.global.apipayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AssistantErrorCode implements BaseCode {
	SEGMENT_NOT_FOUND(HttpStatus.NOT_FOUND,
			"ASSISTANT404_1",
			"세그먼트를 찾을 수 없습니다."),
	MEETING_NOT_FOUND(HttpStatus.NOT_FOUND,
			"ASSISTANT404_2",
			"회의를 찾을 수 없습니다."),
	SEGMENT_MEETING_MISMATCH(HttpStatus.BAD_REQUEST,
			"ASSISTANT400_1",
			"세그먼트가 요청한 회의에 속하지 않습니다."),
	NOT_MEETING_PARTICIPANT(HttpStatus.FORBIDDEN,
			"ASSISTANT403_1",
			"회의 참여자만 힌트를 생성할 수 있습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
