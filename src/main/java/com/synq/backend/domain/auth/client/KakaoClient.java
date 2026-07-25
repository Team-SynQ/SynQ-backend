package com.synq.backend.domain.auth.client;

import com.synq.backend.domain.auth.client.dto.KakaoTokenResponse;
import com.synq.backend.domain.auth.client.dto.KakaoUserResponse;
import com.synq.backend.domain.auth.code.AuthErrorCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class KakaoClient {

	private static final String TOKEN_URI = "https://kauth.kakao.com/oauth/token";
	private static final String USER_ME_URI = "https://kapi.kakao.com/v2/user/me";

	private final RestClient restClient;
	private final KakaoProperties properties;
	private final SocialApiCallExecutor callExecutor;

	public KakaoClient(RestClient.Builder builder, KakaoProperties properties, SocialApiCallExecutor callExecutor) {
		this.restClient = builder.build();
		this.properties = properties;
		this.callExecutor = callExecutor;
	}

	public String exchangeCodeForAccessToken(String code) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "authorization_code");
		form.add("client_id", properties.clientId());
		form.add("redirect_uri", properties.redirectUri());
		form.add("code", code);
		if (StringUtils.hasText(properties.clientSecret())) {
			form.add("client_secret", properties.clientSecret());
		}

		KakaoTokenResponse response = callExecutor.call(() -> restClient.post()
				.uri(TOKEN_URI)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.body(KakaoTokenResponse.class),
				AuthErrorCode.INVALID_KAKAO_LOGIN, AuthErrorCode.KAKAO_SERVICE_UNAVAILABLE);

		if (response == null || !StringUtils.hasText(response.accessToken())) {
			throw new GeneralException(AuthErrorCode.INVALID_KAKAO_LOGIN);
		}
		return response.accessToken();
	}

	public KakaoUserResponse fetchUser(String kakaoAccessToken) {
		KakaoUserResponse response = callExecutor.call(() -> restClient.get()
				.uri(USER_ME_URI)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
				.retrieve()
				.body(KakaoUserResponse.class),
				AuthErrorCode.INVALID_KAKAO_LOGIN, AuthErrorCode.KAKAO_SERVICE_UNAVAILABLE);

		if (response == null || response.id() == null) {
			throw new GeneralException(AuthErrorCode.INVALID_KAKAO_LOGIN);
		}
		return response;
	}
}
