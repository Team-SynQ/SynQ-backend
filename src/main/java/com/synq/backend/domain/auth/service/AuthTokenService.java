package com.synq.backend.domain.auth.service;

import com.synq.backend.domain.auth.code.AuthErrorCode;
import com.synq.backend.domain.auth.dto.TokenResponse;
import com.synq.backend.domain.auth.jwt.JwtProperties;
import com.synq.backend.domain.auth.jwt.JwtProvider;
import com.synq.backend.domain.auth.repository.RefreshTokenRedisRepository;
import com.synq.backend.domain.user.repository.RoleProfileRepository;
import com.synq.backend.global.apipayload.exception.GeneralException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthTokenService {

	private static final Logger log = LoggerFactory.getLogger(AuthTokenService.class);

	private final JwtProvider jwtProvider;
	private final RefreshTokenRedisRepository refreshTokenRepository;
	private final JwtProperties jwtProperties;
	private final RoleProfileRepository roleProfileRepository;

	public AuthTokenService(JwtProvider jwtProvider, RefreshTokenRedisRepository refreshTokenRepository,
							JwtProperties jwtProperties, RoleProfileRepository roleProfileRepository) {
		this.jwtProvider = jwtProvider;
		this.refreshTokenRepository = refreshTokenRepository;
		this.jwtProperties = jwtProperties;
		this.roleProfileRepository = roleProfileRepository;
	}

	public TokenResponse issue(Long userId, boolean isNewUser) {
		boolean onboardingCompleted = isOnboardingCompleted(userId);
		String accessToken = jwtProvider.createAccessToken(userId);
		String refreshToken = UUID.randomUUID().toString();
		refreshTokenRepository.issue(userId, sha256Hex(refreshToken), refreshTokenTtl());
		return new TokenResponse(accessToken, refreshToken, isNewUser, onboardingCompleted);
	}

	public TokenResponse refresh(String rawRefreshToken) {
		String oldTokenHash = sha256Hex(rawRefreshToken);
		Long userId = refreshTokenRepository.findUserIdByTokenHash(oldTokenHash)
				.orElseThrow(() -> new GeneralException(AuthErrorCode.INVALID_REFRESH_TOKEN));
		boolean onboardingCompleted = isOnboardingCompleted(userId);

		String newRefreshToken = UUID.randomUUID().toString();
		boolean rotated = refreshTokenRepository.rotate(userId, oldTokenHash,
				sha256Hex(newRefreshToken), refreshTokenTtl());
		if (!rotated) {
			throw new GeneralException(AuthErrorCode.INVALID_REFRESH_TOKEN);
		}

		String accessToken = jwtProvider.createAccessToken(userId);
		return new TokenResponse(accessToken, newRefreshToken, false, onboardingCompleted);
	}

	// 온보딩 완료 여부는 별도 플래그로 관리하지 않고, "기본 역할·관점 프로필이 있는지"로 판단
	private boolean isOnboardingCompleted(Long userId) {
		return roleProfileRepository.findByUserIdAndIsDefaultTrue(userId).isPresent();
	}

	public void revoke(Long userId) {
		try {
			refreshTokenRepository.revoke(userId);
		} catch (DataAccessException e) {
			log.warn("레디스 장애(연결 실패 또는 타임아웃)로 refresh token 폐기를 건너뜁니다.", e);
		}
	}

	private Duration refreshTokenTtl() {
		return Duration.ofDays(jwtProperties.refreshTokenExpirationDays());
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
