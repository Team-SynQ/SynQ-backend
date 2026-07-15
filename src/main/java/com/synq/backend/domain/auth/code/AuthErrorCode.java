package com.synq.backend.domain.auth.code;

import com.synq.backend.global.apipayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseCode {
	EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT,
			"AUTH409_1",
			"이미 가입된 이메일입니다."),
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED,
			"AUTH401_2",
			"이메일 또는 비밀번호가 올바르지 않습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
