package com.synq.backend.domain.auth.service;

import com.synq.backend.domain.auth.client.GoogleClient;
import com.synq.backend.domain.auth.client.dto.GoogleUserResponse;
import com.synq.backend.domain.auth.dto.TokenResponse;
import com.synq.backend.domain.user.entity.Provider;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GoogleAuthService {

	private static final String DEFAULT_NAME = "구글사용자";
	private static final int MAX_NAME_LENGTH = 20;

	private final GoogleClient googleClient;
	private final UserRepository userRepository;
	private final AuthTokenService authTokenService;

	public GoogleAuthService(GoogleClient googleClient, UserRepository userRepository,
							AuthTokenService authTokenService) {
		this.googleClient = googleClient;
		this.userRepository = userRepository;
		this.authTokenService = authTokenService;
	}

	public TokenResponse login(String code) {
		String googleAccessToken = googleClient.exchangeCodeForAccessToken(code);
		GoogleUserResponse googleUser = googleClient.fetchUser(googleAccessToken);
		String providerId = googleUser.sub();

		Optional<User> existing = userRepository.findByProviderAndProviderId(Provider.GOOGLE, providerId);
		if (existing.isPresent()) {
			return authTokenService.issue(existing.get().getUserId(), false);
		}

		try {
			User user = userRepository.save(User.ofSocial(
					resolveName(googleUser), resolveEmail(googleUser), Provider.GOOGLE, providerId));
			return authTokenService.issue(user.getUserId(), true);
		} catch (DataIntegrityViolationException e) {
			User user = userRepository.findByProviderAndProviderId(Provider.GOOGLE, providerId)
					.orElseThrow(() -> e);
			return authTokenService.issue(user.getUserId(), false);
		}
	}

	private String resolveName(GoogleUserResponse googleUser) {
		String name = googleUser.name() != null ? googleUser.name() : DEFAULT_NAME;
		return SocialNameTruncator.truncate(name, MAX_NAME_LENGTH);
	}

	private String resolveEmail(GoogleUserResponse googleUser) {
		return Boolean.TRUE.equals(googleUser.emailVerified()) ? googleUser.email() : null;
	}
}
