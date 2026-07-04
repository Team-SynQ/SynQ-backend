package com.synq.backend.global.apipayload.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GeneralSuccessCode implements BaseCode {
	REQUEST_OK(HttpStatus.OK,
			"COMMON200",
			"성공적으로 요청을 처리했습니다."),
	CREATED(HttpStatus.CREATED,
			"COMMON201",
			"성공적으로 응답이 생성되었습니다."),
	FOUND(HttpStatus.FOUND,
			"FOUND200",
			"조회에 성공했습니다.")
	;
	private final HttpStatus status;
	private final String code;
	private final String message;
}
