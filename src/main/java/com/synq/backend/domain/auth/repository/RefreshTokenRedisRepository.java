package com.synq.backend.domain.auth.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Repository
public class RefreshTokenRedisRepository {

	private static final String TOKEN_KEY_PREFIX = "refresh:token:";
	private static final String USER_KEY_PREFIX = "refresh:user:";

	private static final DefaultRedisScript<Long> ISSUE_SCRIPT = new DefaultRedisScript<>("""
			local old = redis.call('GET', KEYS[1])
			if old then
			  redis.call('DEL', ARGV[4] .. old)
			end
			redis.call('SET', KEYS[2], ARGV[1], 'EX', ARGV[2])
			redis.call('SET', KEYS[1], ARGV[3], 'EX', ARGV[2])
			return 1
			""", Long.class);

	private static final DefaultRedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>("""
			local current = redis.call('GET', KEYS[1])
			if current == ARGV[1] then
			  redis.call('DEL', ARGV[5] .. ARGV[1])
			  redis.call('SET', KEYS[2], ARGV[2], 'EX', ARGV[3])
			  redis.call('SET', KEYS[1], ARGV[4], 'EX', ARGV[3])
			  return 1
			else
			  return 0
			end
			""", Long.class);

	private static final DefaultRedisScript<Long> REVOKE_SCRIPT = new DefaultRedisScript<>("""
			local hash = redis.call('GET', KEYS[1])
			if hash then
			  redis.call('DEL', ARGV[1] .. hash)
			  redis.call('DEL', KEYS[1])
			end
			return 1
			""", Long.class);

	private final StringRedisTemplate redisTemplate;

	public RefreshTokenRedisRepository(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public void issue(Long userId, String tokenHash, Duration ttl) {
		String userKey = USER_KEY_PREFIX + userId;
		String tokenKey = TOKEN_KEY_PREFIX + tokenHash;
		redisTemplate.execute(ISSUE_SCRIPT, List.of(userKey, tokenKey),
				String.valueOf(userId), String.valueOf(ttl.toSeconds()), tokenHash, TOKEN_KEY_PREFIX);
	}

	public boolean rotate(Long userId, String oldTokenHash, String newTokenHash, Duration ttl) {
		String userKey = USER_KEY_PREFIX + userId;
		String newTokenKey = TOKEN_KEY_PREFIX + newTokenHash;
		Long result = redisTemplate.execute(ROTATE_SCRIPT, List.of(userKey, newTokenKey),
				oldTokenHash, String.valueOf(userId), String.valueOf(ttl.toSeconds()), newTokenHash, TOKEN_KEY_PREFIX);
		return result != null && result == 1;
	}

	public void revoke(Long userId) {
		redisTemplate.execute(REVOKE_SCRIPT, List.of(USER_KEY_PREFIX + userId), TOKEN_KEY_PREFIX);
	}

	public Optional<Long> findUserIdByTokenHash(String tokenHash) {
		String value = redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + tokenHash);
		return Optional.ofNullable(value).map(Long::valueOf);
	}
}
