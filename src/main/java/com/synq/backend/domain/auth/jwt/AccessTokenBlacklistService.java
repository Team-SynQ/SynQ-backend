package com.synq.backend.domain.auth.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

@Service
public class AccessTokenBlacklistService {

	private static final Logger log = LoggerFactory.getLogger(AccessTokenBlacklistService.class);
	private static final String KEY_PREFIX = "blacklist:accessToken:";

	private final StringRedisTemplate redisTemplate;

	public AccessTokenBlacklistService(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public void blacklist(String accessToken, Duration remainingValidity) {
		if (remainingValidity.isZero() || remainingValidity.isNegative()) {
			return;
		}
		redisTemplate.opsForValue().set(KEY_PREFIX + sha256Hex(accessToken), "1", remainingValidity);
	}

	public boolean isBlacklisted(String accessToken) {
		try {
			return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + sha256Hex(accessToken)));
		} catch (DataAccessException e) {
			log.warn("레디스 장애(연결 실패 또는 타임아웃)로 블랙리스트 확인을 건너뜁니다.", e);
			return false;
		}
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
