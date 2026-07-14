package com.synq.backend.domain.ai.client.openai;

import com.synq.backend.global.apipayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum OpenAiErrorCode implements BaseCode {
	EMPTY_INPUT(HttpStatus.BAD_REQUEST,
			"OPENAI400_1",
			"OpenAI 요청 입력값이 비어 있습니다."),
	API_KEY_MISSING(HttpStatus.INTERNAL_SERVER_ERROR,
			"OPENAI500_1",
			"OpenAI API Key가 설정되지 않았습니다."),
	REQUEST_FAILED(HttpStatus.BAD_GATEWAY,
			"OPENAI502_1",
			"OpenAI API 호출에 실패했습니다."),
	INVALID_RESPONSE(HttpStatus.BAD_GATEWAY,
			"OPENAI502_2",
			"OpenAI API 응답 형식이 올바르지 않습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
