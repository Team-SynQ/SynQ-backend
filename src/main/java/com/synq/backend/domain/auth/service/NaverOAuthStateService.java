package com.synq.backend.domain.auth.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class NaverOAuthStateService {

	private static final String KEY_PREFIX = "naver:oauth-state:";
	private static final Duration STATE_TTL = Duration.ofMinutes(5);

	private final StringRedisTemplate redisTemplate;

	public NaverOAuthStateService(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public String issue() {
		String state = UUID.randomUUID().toString();
		redisTemplate.opsForValue().set(KEY_PREFIX + state, "1", STATE_TTL);
		return state;
	}

	// Redis의 DEL은 원자적이라, 두 요청이 동시에 같은 state로 들어와도 하나만 성공한다.
	public boolean validateAndConsume(String state) {
		return Boolean.TRUE.equals(redisTemplate.delete(KEY_PREFIX + state));
	}
}
