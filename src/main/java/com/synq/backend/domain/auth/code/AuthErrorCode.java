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
			"카카오 로그인에 실패했습니다."),
	KAKAO_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE,
			"AUTH503_1",
			"카카오 서버 응답이 원활하지 않습니다. 잠시 후 다시 시도해주세요."),
	INVALID_NAVER_LOGIN(HttpStatus.UNAUTHORIZED,
			"AUTH401_7",
			"네이버 로그인에 실패했습니다."),
	NAVER_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE,
			"AUTH503_2",
			"네이버 서버 응답이 원활하지 않습니다. 잠시 후 다시 시도해주세요."),
	INVALID_OAUTH_STATE(HttpStatus.UNAUTHORIZED,
			"AUTH401_8",
			"유효하지 않거나 만료된 state입니다."),
	INVALID_GOOGLE_LOGIN(HttpStatus.UNAUTHORIZED,
			"AUTH401_9",
			"구글 로그인에 실패했습니다."),
	GOOGLE_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE,
			"AUTH503_3",
			"구글 서버 응답이 원활하지 않습니다. 잠시 후 다시 시도해주세요.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
