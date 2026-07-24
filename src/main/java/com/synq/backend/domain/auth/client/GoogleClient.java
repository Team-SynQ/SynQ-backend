package com.synq.backend.domain.auth.client;

import com.synq.backend.domain.auth.client.dto.GoogleTokenResponse;
import com.synq.backend.domain.auth.client.dto.GoogleUserResponse;
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
public class GoogleClient {

	private static final String TOKEN_URI = "https://oauth2.googleapis.com/token";
	private static final String USER_INFO_URI = "https://www.googleapis.com/oauth2/v3/userinfo";

	private final RestClient restClient;
	private final GoogleProperties properties;
	private final SocialApiCallExecutor callExecutor;

	public GoogleClient(RestClient.Builder builder, GoogleProperties properties, SocialApiCallExecutor callExecutor) {
		this.restClient = builder.build();
		this.properties = properties;
		this.callExecutor = callExecutor;
	}

	public String exchangeCodeForAccessToken(String code) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "authorization_code");
		form.add("client_id", properties.clientId());
		form.add("client_secret", properties.clientSecret());
		form.add("redirect_uri", properties.redirectUri());
		form.add("code", code);

		GoogleTokenResponse response = callExecutor.call(() -> restClient.post()
				.uri(TOKEN_URI)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.body(GoogleTokenResponse.class),
				AuthErrorCode.INVALID_GOOGLE_LOGIN, AuthErrorCode.GOOGLE_SERVICE_UNAVAILABLE);

		if (response == null || !StringUtils.hasText(response.accessToken())) {
			throw new GeneralException(AuthErrorCode.INVALID_GOOGLE_LOGIN);
		}
		return response.accessToken();
	}

	public GoogleUserResponse fetchUser(String googleAccessToken) {
		GoogleUserResponse response = callExecutor.call(() -> restClient.get()
				.uri(USER_INFO_URI)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + googleAccessToken)
				.retrieve()
				.body(GoogleUserResponse.class),
				AuthErrorCode.INVALID_GOOGLE_LOGIN, AuthErrorCode.GOOGLE_SERVICE_UNAVAILABLE);

		if (response == null || !StringUtils.hasText(response.sub())) {
			throw new GeneralException(AuthErrorCode.INVALID_GOOGLE_LOGIN);
		}
		return response;
	}
}
