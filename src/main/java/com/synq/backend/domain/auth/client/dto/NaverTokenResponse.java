package com.synq.backend.domain.auth.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;


public record NaverTokenResponse(
		@JsonProperty("access_token") String accessToken
) {
}
