package com.synq.backend.domain.user.repository;

import com.synq.backend.domain.user.entity.Provider;
import com.synq.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByProviderAndProviderId(Provider provider, String providerId);

	// LOCAL 전용 조회
	Optional<User> findByProviderAndEmail(Provider provider, String email);
}
