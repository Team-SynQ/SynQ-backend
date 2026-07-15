package com.synq.backend.domain.ai.client.openai;

import com.synq.backend.global.apipayload.code.BaseCode;
import com.synq.backend.global.apipayload.exception.GeneralException;

/**
 * OpenAI 연동 실패를 공통 예외 응답으로 처리하기 위한 예외.
 */
public class OpenAiException extends GeneralException {

	public OpenAiException(BaseCode code) {
		super(code);
	}

	public OpenAiException(BaseCode code, Throwable cause) {
		super(code);
		initCause(cause);
	}
}
