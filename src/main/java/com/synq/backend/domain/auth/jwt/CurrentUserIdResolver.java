package com.synq.backend.domain.auth.jwt;

import com.synq.backend.domain.auth.code.AuthErrorCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Component;

/**
 * Authorization 헤더의 access token을 검증하고 현재 사용자 ID를 반환한다.
 */
@Component
public class CurrentUserIdResolver {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtProvider jwtProvider;

	public CurrentUserIdResolver(JwtProvider jwtProvider) {
		this.jwtProvider = jwtProvider;
	}

	public Long resolve(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new GeneralException(AuthErrorCode.INVALID_ACCESS_TOKEN);
		}

		String accessToken = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
		if (accessToken.isEmpty()) {
			throw new GeneralException(AuthErrorCode.INVALID_ACCESS_TOKEN);
		}

		try {
			return jwtProvider.parseUserId(accessToken);
		} catch (JwtException | IllegalArgumentException exception) {
			throw new GeneralException(AuthErrorCode.INVALID_ACCESS_TOKEN, exception);
		}
	}
}
