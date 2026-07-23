package com.synq.backend.domain.auth.service;

import com.synq.backend.domain.auth.client.KakaoClient;
import com.synq.backend.domain.auth.client.dto.KakaoUserResponse;
import com.synq.backend.domain.auth.dto.TokenResponse;
import com.synq.backend.domain.user.entity.Provider;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class KakaoAuthService {

	private static final String DEFAULT_NAME = "카카오사용자";
	// users.name 컬럼 길이(VARCHAR(20))와 맞춘다. 카카오 닉네임은 길이 제한이 없다.
	private static final int MAX_NAME_LENGTH = 20;

	private final KakaoClient kakaoClient;
	private final UserRepository userRepository;
	private final AuthTokenService authTokenService;

	public KakaoAuthService(KakaoClient kakaoClient, UserRepository userRepository,
							AuthTokenService authTokenService) {
		this.kakaoClient = kakaoClient;
		this.userRepository = userRepository;
		this.authTokenService = authTokenService;
	}

	public TokenResponse login(String code) {
		String kakaoAccessToken = kakaoClient.exchangeCodeForAccessToken(code);
		KakaoUserResponse kakaoUser = kakaoClient.fetchUser(kakaoAccessToken);
		String providerId = String.valueOf(kakaoUser.id());

		Optional<User> existing = userRepository.findByProviderAndProviderId(Provider.KAKAO, providerId);
		if (existing.isPresent()) {
			return authTokenService.issue(existing.get().getUserId(), false);
		}

		try {
			User user = userRepository.save(User.ofSocial(
					resolveName(kakaoUser), resolveEmail(kakaoUser), Provider.KAKAO, providerId));
			return authTokenService.issue(user.getUserId(), true);
		} catch (DataIntegrityViolationException e) {
			User user = userRepository.findByProviderAndProviderId(Provider.KAKAO, providerId)
					.orElseThrow(() -> e);
			return authTokenService.issue(user.getUserId(), false);
		}
	}

	private String resolveName(KakaoUserResponse kakaoUser) {
		String nickname = Optional.ofNullable(kakaoUser.kakaoAccount())
				.map(KakaoUserResponse.KakaoAccount::profile)
				.map(KakaoUserResponse.Profile::nickname)
				.orElse(DEFAULT_NAME);
		return SocialNameTruncator.truncate(nickname, MAX_NAME_LENGTH);
	}

	private String resolveEmail(KakaoUserResponse kakaoUser) {
		return Optional.ofNullable(kakaoUser.kakaoAccount())
				.map(KakaoUserResponse.KakaoAccount::email)
				.orElse(null);
	}
}
