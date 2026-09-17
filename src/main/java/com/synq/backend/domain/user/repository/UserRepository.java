package com.synq.backend.domain.user.repository;

import com.synq.backend.domain.user.entity.Provider;
import com.synq.backend.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByProviderAndProviderId(Provider provider, String providerId);

	// LOCAL 전용 조회
	Optional<User> findByProviderAndEmail(Provider provider, String email);

	boolean existsByUserIdAndDeletedAtIsNull(Long userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT user FROM User user WHERE user.userId = :userId AND user.deletedAt IS NULL")
	Optional<User> findActiveByIdForUpdate(@Param("userId") Long userId);
}
