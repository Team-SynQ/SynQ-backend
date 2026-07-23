package com.synq.backend.domain.auth.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleTokenResponse(
		@JsonProperty("access_token") String accessToken
) {
}
