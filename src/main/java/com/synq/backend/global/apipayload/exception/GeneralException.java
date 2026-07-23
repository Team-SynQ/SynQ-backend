package com.synq.backend.global.apipayload.exception;

import com.synq.backend.global.apipayload.code.BaseCode;
import lombok.Getter;

@Getter
public class GeneralException extends RuntimeException {

	private final BaseCode code;

	public GeneralException(BaseCode code) {
		super(code.getMessage());
		this.code = code;
	}

	public GeneralException(BaseCode code, Throwable cause) {
		super(code.getMessage(), cause);
		this.code = code;
	}
}
