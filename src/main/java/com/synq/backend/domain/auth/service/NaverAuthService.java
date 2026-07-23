package com.synq.backend.domain.auth.service;

import com.synq.backend.domain.auth.client.NaverClient;
import com.synq.backend.domain.auth.client.dto.NaverUserResponse;
import com.synq.backend.domain.auth.dto.TokenResponse;
import com.synq.backend.domain.user.entity.Provider;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
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

	public NaverAuthService(NaverClient naverClient, UserRepository userRepository,
							AuthTokenService authTokenService) {
		this.naverClient = naverClient;
		this.userRepository = userRepository;
		this.authTokenService = authTokenService;
	}

	public TokenResponse login(String code, String state) {
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


	// 실명(회원이름) 동의항목은 요청하지 않는다 - 닉네임만 쓴다.
	private String resolveName(NaverUserResponse naverUser) {
		String nickname = naverUser.response().nickname();
		String name = StringUtils.hasText(nickname) ? nickname : DEFAULT_NAME;
		return name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name;
	}

	private String resolveEmail(NaverUserResponse naverUser) {
		return naverUser.response().email();
	}
}
