package com.synq.backend.domain.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtProvider {

	private final SecretKey key;
	private final long accessTokenExpirationMillis;
	private final AccessTokenBlacklistService blacklistService;

	public JwtProvider(JwtProperties properties, AccessTokenBlacklistService blacklistService) {
		this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
		this.accessTokenExpirationMillis = properties.accessTokenExpirationMinutes() * 60 * 1000;
		this.blacklistService = blacklistService;
	}

	public String createAccessToken(Long userId) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(String.valueOf(userId))
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusMillis(accessTokenExpirationMillis)))
				.signWith(key)
				.compact();
	}

	public Long parseUserId(String accessToken) {
		Long userId = parseSubject(accessToken);
		if (blacklistService.isBlacklisted(accessToken)) {
			throw new JwtException("로그아웃 처리된 access token입니다.");
		}
		return userId;
	}

	public Long parseUserIdIgnoringExpiration(String accessToken) {
		try {
			return parseSubject(accessToken);
		} catch (ExpiredJwtException e) {
			return Long.valueOf(e.getClaims().getSubject());
		}
	}

	private Long parseSubject(String accessToken) {
		Claims claims = Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(accessToken)
				.getPayload();
		return Long.valueOf(claims.getSubject());
	}

	public Duration getRemainingValidity(String accessToken) {
		try {
			Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(accessToken).getPayload();
			return remaining(claims);
		} catch (ExpiredJwtException e) {
			return remaining(e.getClaims());
		} catch (JwtException | IllegalArgumentException e) {
			return Duration.ZERO;
		}
	}

	private Duration remaining(Claims claims) {
		Instant expiration = claims.getExpiration().toInstant();
		Instant now = Instant.now();
		return expiration.isAfter(now) ? Duration.between(now, expiration) : Duration.ZERO;
	}

	public boolean isValid(String accessToken) {
		try {
			parseUserId(accessToken);
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			return false;
		}
	}
}
