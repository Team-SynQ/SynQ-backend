package com.synq.backend.domain.user.repository;

import com.synq.backend.domain.user.entity.RoleProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleProfileRepository extends JpaRepository<RoleProfile, Long> {

	List<RoleProfile> findAllByUserIdOrderByCreatedAtAsc(Long userId);

	Optional<RoleProfile> findByIdAndUserId(Long id, Long userId);

	boolean existsByUserId(Long userId);

	Optional<RoleProfile> findByUserIdAndIsDefaultTrue(Long userId);
}
