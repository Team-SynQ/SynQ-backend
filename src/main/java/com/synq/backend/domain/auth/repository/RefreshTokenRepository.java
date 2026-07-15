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

	void deleteByUserId(Long userId);
}
