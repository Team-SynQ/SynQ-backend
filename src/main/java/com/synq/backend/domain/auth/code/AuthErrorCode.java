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
			"이메일 또는 비밀번호가 올바르지 않습니다."),
	INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED,
			"AUTH401_3",
			"유효하지 않은 refresh token입니다."),
	REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED,
			"AUTH401_4",
			"만료된 refresh token입니다."),
	INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED,
			"AUTH401_5",
			"유효하지 않은 access token입니다."),
	INVALID_KAKAO_LOGIN(HttpStatus.UNAUTHORIZED,
			"AUTH401_6",
			"카카오 로그인에 실패했습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
