package com.synq.backend.domain.auth.controller;

import com.synq.backend.domain.auth.dto.GoogleLoginRequest;
import com.synq.backend.domain.auth.dto.KakaoLoginRequest;
import com.synq.backend.domain.auth.dto.LoginRequest;
import com.synq.backend.domain.auth.dto.NaverLoginRequest;
import com.synq.backend.domain.auth.dto.NaverStateResponse;
import com.synq.backend.domain.auth.dto.RefreshRequest;
import com.synq.backend.domain.auth.dto.SignupRequest;
import com.synq.backend.domain.auth.dto.TokenResponse;
import com.synq.backend.global.apipayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Auth", description = "인증")
public interface AuthControllerDocs {

	@Operation(summary = "토큰 재발급",
			description = "refreshToken으로 access token과 refresh token을 재발급한다. "
					+ "refresh token은 1회용이라 재발급 시 기존 값은 폐기되고 새 값으로 교체된다(rotation).")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "재발급 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "refreshToken이 비어있음"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
					description = "refresh token이 유효하지 않거나 만료됨")
	})
	ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshRequest request);

	@Operation(summary = "로그아웃",
			description = "현재 access token을 블랙리스트에 등록해 즉시 무효화하고, 서버에 저장된 refresh token을 폐기한다.")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "access token이 없거나 유효하지 않음")
	})
	ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request);
}

@Tag(name = "Auth", description = "인증")
interface EmailAuthControllerDocs {

	@Operation(summary = "이메일 회원가입 (개발용)",
			description = "이메일/비밀번호로 회원가입한다. 실제 서비스는 카카오/네이버/구글 소셜 로그인만 지원하며, "
					+ "이 API는 개발·테스트 편의를 위한 것으로 운영 환경에서는 사용하지 않는다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "회원가입 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이름/이메일/비밀번호 형식이 올바르지 않음"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 가입된 이메일")
	})
	ResponseEntity<ApiResponse<TokenResponse>> signup(@Valid @RequestBody SignupRequest request);

	@Operation(summary = "이메일 로그인 (개발용)",
			description = "이메일/비밀번호로 로그인한다. 실제 서비스는 카카오/네이버/구글 소셜 로그인만 지원하며, "
					+ "이 API는 개발·테스트 편의를 위한 것으로 운영 환경에서는 사용하지 않는다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호가 올바르지 않음")
	})
	ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request);
}

@Tag(name = "Auth", description = "인증")
interface KakaoAuthControllerDocs {

	@Operation(summary = "카카오 로그인",
			description = "카카오 OAuth 인가 코드(code)로 로그인한다. 최초 로그인이면 회원가입도 함께 처리된다. "
					+ "redirectUri에는 인가 요청에 사용한 값을 그대로 담아야 하며, 다르면 카카오가 토큰 교환을 거부한다. "
					+ "생략하면 서버에 설정된 기본 redirect URI를 사용한다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "code가 비어있음"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "카카오 로그인 실패 (code 만료/무효 등)"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "카카오 서버 응답 지연/장애")
	})
	ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody KakaoLoginRequest request);
}

@Tag(name = "Auth", description = "인증")
interface NaverAuthControllerDocs {

	@Operation(summary = "네이버 로그인용 state 발급",
			description = "네이버 OAuth 인가 요청에 실어 보낼 CSRF 방지용 state 값을 발급한다. "
					+ "프론트는 이 값을 인가 요청의 state 파라미터로 사용하고, 콜백에서 받은 state를 그대로 로그인 API에 전달해야 한다. "
					+ "발급된 state는 일정 시간이 지나면 만료된다. ")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "발급 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "state 저장 실패")
	})
	ResponseEntity<ApiResponse<NaverStateResponse>> issueState();

	@Operation(summary = "네이버 로그인",
			description = "네이버 OAuth 인가 코드(code)와 state로 로그인한다. state는 /auth/naver/state로 미리 발급받은 값과 "
					+ "일치해야 하며(CSRF 방지), 일치하지 않거나 만료된 경우 로그인이 거부된다.  최초 로그인이면 회원가입도 함께 처리된다. "
					+ "redirectUri에는 인가 요청에 사용한 값을 그대로 담아야 하며, 다르면 네이버가 토큰 교환을 거부한다. "
					+ "생략하면 서버에 설정된 기본 redirect URI를 사용한다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "code 또는 state가 비어있음"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
					description = "state가 유효하지 않거나 만료됨, 또는 네이버 로그인 실패"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "네이버 서버 응답 지연/장애")
	})
	ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody NaverLoginRequest request);
}

@Tag(name = "Auth", description = "인증")
interface GoogleAuthControllerDocs {

	@Operation(summary = "구글 로그인",
			description = "구글 OAuth 인가 코드(code)로 로그인한다. 최초 로그인이면 회원가입도 함께 처리된다. "
					+ "redirectUri에는 인가 요청에 사용한 값을 그대로 담아야 하며, 다르면 구글이 토큰 교환을 거부한다. "
					+ "생략하면 서버에 설정된 기본 redirect URI를 사용한다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "code가 비어있음"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "구글 로그인 실패 (code 만료/무효 등)"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "구글 서버 응답 지연/장애")
	})
	ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody GoogleLoginRequest request);
}
