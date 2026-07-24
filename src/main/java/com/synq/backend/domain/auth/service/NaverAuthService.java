package com.synq.backend.domain.auth.service;

import com.synq.backend.domain.auth.client.NaverClient;
import com.synq.backend.domain.auth.client.dto.NaverUserResponse;
import com.synq.backend.domain.auth.code.AuthErrorCode;
import com.synq.backend.domain.auth.dto.TokenResponse;
import com.synq.backend.domain.user.entity.Provider;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.global.apipayload.exception.GeneralException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class NaverAuthService {

	private static final String DEFAULT_NAME = "사용자";
	private static final int MAX_NAME_LENGTH = 20;

	private final NaverClient naverClient;
	private final UserRepository userRepository;
	private final AuthTokenService authTokenService;
	private final NaverOAuthStateService naverOAuthStateService;

	public NaverAuthService(NaverClient naverClient, UserRepository userRepository,
							AuthTokenService authTokenService, NaverOAuthStateService naverOAuthStateService) {
		this.naverClient = naverClient;
		this.userRepository = userRepository;
		this.authTokenService = authTokenService;
		this.naverOAuthStateService = naverOAuthStateService;
	}

	public TokenResponse login(String code, String state) {
		if (!naverOAuthStateService.validateAndConsume(state)) {
			throw new GeneralException(AuthErrorCode.INVALID_OAUTH_STATE);
		}
		String naverAccessToken = naverClient.exchangeCodeForAccessToken(code, state);
		NaverUserResponse naverUser = naverClient.fetchUser(naverAccessToken);
		String providerId = naverUser.response().id();

		Optional<User> existing = userRepository.findByProviderAndProviderId(Provider.NAVER, providerId);
		if (existing.isPresent()) {
			return authTokenService.issue(existing.get().getUserId(), false);
		}

		try {
			User user = userRepository.save(User.ofSocial(
					resolveName(naverUser), resolveEmail(naverUser), Provider.NAVER, providerId));
			return authTokenService.issue(user.getUserId(), true);
		} catch (DataIntegrityViolationException e) {
			User user = userRepository.findByProviderAndProviderId(Provider.NAVER, providerId)
					.orElseThrow(() -> e);
			return authTokenService.issue(user.getUserId(), false);
		}
	}


	private String resolveName(NaverUserResponse naverUser) {
		String nickname = naverUser.response().nickname();
		String name = StringUtils.hasText(nickname) ? nickname : DEFAULT_NAME;
		return name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name;
	}

	private String resolveEmail(NaverUserResponse naverUser) {
		return naverUser.response().email();
	}
}
