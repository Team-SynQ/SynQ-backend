package com.synq.backend.domain.auth.service;

import com.synq.backend.domain.auth.code.AuthErrorCode;
import com.synq.backend.domain.auth.dto.TokenResponse;
import com.synq.backend.domain.auth.entity.RefreshToken;
import com.synq.backend.domain.auth.jwt.JwtProperties;
import com.synq.backend.domain.auth.jwt.JwtProvider;
import com.synq.backend.domain.auth.repository.RefreshTokenRepository;
import com.synq.backend.global.apipayload.exception.GeneralException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthTokenService {

	private final JwtProvider jwtProvider;
	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtProperties jwtProperties;

	public AuthTokenService(JwtProvider jwtProvider, RefreshTokenRepository refreshTokenRepository,
							JwtProperties jwtProperties) {
		this.jwtProvider = jwtProvider;
		this.refreshTokenRepository = refreshTokenRepository;
		this.jwtProperties = jwtProperties;
	}

	@Transactional
	public TokenResponse issue(Long userId, boolean isNewUser) {
		String accessToken = jwtProvider.createAccessToken(userId);

		String refreshToken = UUID.randomUUID().toString();
		OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(jwtProperties.refreshTokenExpirationDays());
		refreshTokenRepository.upsert(userId, sha256Hex(refreshToken), expiresAt);

		return new TokenResponse(accessToken, refreshToken, isNewUser);
	}

	@Transactional
	public TokenResponse refresh(String rawRefreshToken) {
		RefreshToken stored = refreshTokenRepository.findByTokenHash(sha256Hex(rawRefreshToken))
				.orElseThrow(() -> new GeneralException(AuthErrorCode.INVALID_REFRESH_TOKEN));

		if (stored.isExpired(OffsetDateTime.now())) {
			throw new GeneralException(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
		}

		return issue(stored.getUserId(), false);
	}

	@Transactional
	public void revoke(Long userId) {
		refreshTokenRepository.deleteByUserId(userId);
	}

	private static String sha256Hex(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
