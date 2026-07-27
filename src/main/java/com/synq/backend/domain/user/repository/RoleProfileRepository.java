package com.synq.backend.domain.user.repository;

import com.synq.backend.domain.user.entity.RoleProfile;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoleProfileRepository extends JpaRepository<RoleProfile, Long> {

	List<RoleProfile> findAllByUserIdOrderByCreatedAtAsc(Long userId);

	Optional<RoleProfile> findByIdAndUserId(Long id, Long userId);

	boolean existsByUserId(Long userId);

	Optional<RoleProfile> findByUserIdAndIsDefaultTrue(Long userId);
    
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT p FROM RoleProfile p WHERE p.id = :id AND p.userId = :userId")
	Optional<RoleProfile> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);
}
