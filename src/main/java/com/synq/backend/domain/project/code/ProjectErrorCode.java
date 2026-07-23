package com.synq.backend.domain.project.code;

import com.synq.backend.global.apipayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ProjectErrorCode implements BaseCode {
	USER_NOT_FOUND(HttpStatus.NOT_FOUND,
			"PROJECT404_1",
			"사용자를 찾을 수 없습니다."),
	USER_PROJECT_LIMIT_EXCEEDED(HttpStatus.CONFLICT,
			"PROJECT409_1",
			"사용자별 최대 프로젝트 수를 초과했습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
