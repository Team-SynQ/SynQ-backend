package com.synq.backend.domain.auth.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleUserResponse(
		String sub,
		String email,
		@JsonProperty("email_verified") Boolean emailVerified,
		String name
) {
}
