package com.synq.backend.domain.reference.code;

import com.synq.backend.global.apipayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ReferenceErrorCode implements BaseCode {
	REFERENCE_LIMIT_EXCEEDED(HttpStatus.CONFLICT,
			"REFERENCE409_1",
			"프로젝트별 최대 참고자료 수를 초과했습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
