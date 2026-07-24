package com.synq.backend.domain.auth.client;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "naver")
public record NaverProperties(
		@NotBlank String clientId,
		@NotBlank String redirectUri,
		@NotBlank String clientSecret
) {
}
