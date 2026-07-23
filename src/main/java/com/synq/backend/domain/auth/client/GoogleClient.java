package com.synq.backend.domain.auth.client;

import com.synq.backend.domain.auth.client.dto.GoogleTokenResponse;
import com.synq.backend.domain.auth.client.dto.GoogleUserResponse;
import com.synq.backend.domain.auth.code.AuthErrorCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.function.Supplier;

@Component
public class GoogleClient {

	private static final String TOKEN_URI = "https://oauth2.googleapis.com/token";
	private static final String USER_INFO_URI = "https://www.googleapis.com/oauth2/v3/userinfo";

	private final RestClient restClient;
	private final GoogleProperties properties;

	public GoogleClient(RestClient.Builder builder, GoogleProperties properties) {
		this.restClient = builder.build();
		this.properties = properties;
	}

	public String exchangeCodeForAccessToken(String code) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "authorization_code");
		form.add("client_id", properties.clientId());
		form.add("client_secret", properties.clientSecret());
		form.add("redirect_uri", properties.redirectUri());
		form.add("code", code);

		GoogleTokenResponse response = call(() -> restClient.post()
				.uri(TOKEN_URI)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.body(GoogleTokenResponse.class));

		if (response == null || !StringUtils.hasText(response.accessToken())) {
			throw new GeneralException(AuthErrorCode.INVALID_GOOGLE_LOGIN);
		}
		return response.accessToken();
	}

	public GoogleUserResponse fetchUser(String googleAccessToken) {
		GoogleUserResponse response = call(() -> restClient.get()
				.uri(USER_INFO_URI)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + googleAccessToken)
				.retrieve()
				.body(GoogleUserResponse.class));

		if (response == null || !StringUtils.hasText(response.sub())) {
			throw new GeneralException(AuthErrorCode.INVALID_GOOGLE_LOGIN);
		}
		return response;
	}

	private <T> T call(Supplier<T> request) {
		try {
			return request.get();
		} catch (HttpClientErrorException e) {
			throw new GeneralException(AuthErrorCode.INVALID_GOOGLE_LOGIN, e);
		} catch (HttpServerErrorException | ResourceAccessException | HttpMessageNotReadableException e) {
			throw new GeneralException(AuthErrorCode.GOOGLE_SERVICE_UNAVAILABLE, e);
		} catch (RestClientException e) {
			throw new GeneralException(AuthErrorCode.INVALID_GOOGLE_LOGIN, e);
		}
	}
}
