package com.synq.backend.domain.auth.service;

import com.synq.backend.domain.auth.dto.TokenResponse;
import com.synq.backend.domain.auth.entity.RefreshToken;
import com.synq.backend.domain.auth.jwt.JwtProperties;
import com.synq.backend.domain.auth.jwt.JwtProvider;
import com.synq.backend.domain.auth.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
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
        
		refreshTokenRepository.deleteByUserId(userId);

		String refreshToken = UUID.randomUUID().toString();
		OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(jwtProperties.refreshTokenExpirationDays());
		refreshTokenRepository.save(RefreshToken.of(userId, refreshToken, expiresAt));

		return new TokenResponse(accessToken, refreshToken, isNewUser);
	}
}
