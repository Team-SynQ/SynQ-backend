package com.synq.backend.domain.auth.client;

import com.synq.backend.domain.auth.client.dto.NaverTokenResponse;
import com.synq.backend.domain.auth.client.dto.NaverUserResponse;
import com.synq.backend.domain.auth.code.AuthErrorCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class NaverClient {

	private static final String TOKEN_URI = "https://nid.naver.com/oauth2.0/token";
	private static final String USER_ME_URI = "https://openapi.naver.com/v1/nid/me";

	private final RestClient restClient;
	private final NaverProperties properties;
	private final SocialApiCallExecutor callExecutor;

	public NaverClient(RestClient.Builder builder, NaverProperties properties, SocialApiCallExecutor callExecutor) {
		this.restClient = builder.build();
		this.properties = properties;
		this.callExecutor = callExecutor;
	}

	public String exchangeCodeForAccessToken(String code, String state) {
		URI uri = UriComponentsBuilder.fromUriString(TOKEN_URI)
				.queryParam("grant_type", "authorization_code")
				.queryParam("client_id", properties.clientId())
				.queryParam("client_secret", properties.clientSecret())
				.queryParam("redirect_uri", properties.redirectUri())
				.queryParam("code", code)
				.queryParam("state", state)
				.encode()
				.build()
				.toUri();

		NaverTokenResponse response = callExecutor.call(() -> restClient.get()
				.uri(uri)
				.retrieve()
				.body(NaverTokenResponse.class),
				AuthErrorCode.INVALID_NAVER_LOGIN, AuthErrorCode.NAVER_SERVICE_UNAVAILABLE);

		if (response == null || !StringUtils.hasText(response.accessToken())) {
			throw new GeneralException(AuthErrorCode.INVALID_NAVER_LOGIN);
		}
		return response.accessToken();
	}

	public NaverUserResponse fetchUser(String naverAccessToken) {
		NaverUserResponse response = callExecutor.call(() -> restClient.get()
				.uri(USER_ME_URI)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + naverAccessToken)
				.retrieve()
				.body(NaverUserResponse.class),
				AuthErrorCode.INVALID_NAVER_LOGIN, AuthErrorCode.NAVER_SERVICE_UNAVAILABLE);

		if (response == null || response.response() == null || response.response().id() == null) {
			throw new GeneralException(AuthErrorCode.INVALID_NAVER_LOGIN);
		}
		return response;
	}
}
