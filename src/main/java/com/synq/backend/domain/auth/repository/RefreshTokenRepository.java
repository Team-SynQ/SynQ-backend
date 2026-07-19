package com.synq.backend.domain.auth.repository;

import com.synq.backend.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			INSERT INTO refresh_tokens (user_id, token_hash, expires_at)
			VALUES (:userId, :tokenHash, :expiresAt)
			ON CONFLICT (user_id) DO UPDATE
			SET token_hash = EXCLUDED.token_hash,
			    expires_at = EXCLUDED.expires_at,
			    created_at = now()
			""", nativeQuery = true)
	void upsert(@Param("userId") Long userId, @Param("tokenHash") String tokenHash,
				@Param("expiresAt") OffsetDateTime expiresAt);

	// compare-and-swap: 넘겨준 oldTokenHash가 그 사이 바뀌거나(동시 refresh) 삭제되지(logout) 않았을 때만 회전에 성공한다.
	// 영향받은 행 수가 0이면 다른 요청이 먼저 회전시켰거나 이미 로그아웃된 것이므로 호출부에서 실패로 처리해야 한다.
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE refresh_tokens
			SET token_hash = :newTokenHash, expires_at = :expiresAt, created_at = now()
			WHERE user_id = :userId AND token_hash = :oldTokenHash
			""", nativeQuery = true)
	int rotate(@Param("userId") Long userId, @Param("oldTokenHash") String oldTokenHash,
				@Param("newTokenHash") String newTokenHash, @Param("expiresAt") OffsetDateTime expiresAt);

	void deleteByUserId(Long userId);
}
