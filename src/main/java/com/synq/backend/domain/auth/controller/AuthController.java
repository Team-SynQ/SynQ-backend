package com.synq.backend.domain.auth.controller;

import com.synq.backend.domain.auth.code.AuthErrorCode;
import com.synq.backend.domain.auth.dto.RefreshRequest;
import com.synq.backend.domain.auth.dto.TokenResponse;
import com.synq.backend.domain.auth.jwt.JwtProvider;
import com.synq.backend.domain.auth.service.AuthTokenService;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralSuccessCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증")
@RestController
@RequestMapping("/auth")
public class AuthController {

	private static final String BEARER_PREFIX = "Bearer ";

	private final AuthTokenService authTokenService;
	private final JwtProvider jwtProvider;

	public AuthController(AuthTokenService authTokenService, JwtProvider jwtProvider) {
		this.authTokenService = authTokenService;
		this.jwtProvider = jwtProvider;
	}

	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
		TokenResponse response = authTokenService.refresh(request.refreshToken());
		return ResponseEntity.status(GeneralSuccessCode.REQUEST_OK.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, response));
	}

	@SecurityRequirement(name = "bearerAuth")
	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
		Long userId = parseUserId(request.getHeader("Authorization"));
		authTokenService.revoke(userId);
		return ResponseEntity.status(GeneralSuccessCode.REQUEST_OK.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null));
	}

	private Long parseUserId(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new GeneralException(AuthErrorCode.INVALID_ACCESS_TOKEN);
		}
		try {
			return jwtProvider.parseUserId(authorizationHeader.substring(BEARER_PREFIX.length()));
		} catch (JwtException | IllegalArgumentException e) {
			throw new GeneralException(AuthErrorCode.INVALID_ACCESS_TOKEN);
		}
	}
}
