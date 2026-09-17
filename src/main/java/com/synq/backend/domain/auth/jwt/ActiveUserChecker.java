package com.synq.backend.domain.auth.jwt;

import com.synq.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActiveUserChecker {

	private final UserRepository userRepository;

	public boolean isActive(Long userId) {
		return userId != null && userRepository.existsByUserIdAndDeletedAtIsNull(userId);
	}
}
