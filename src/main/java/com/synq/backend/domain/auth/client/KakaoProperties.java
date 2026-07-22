package com.synq.backend.domain.auth.client;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "kakao")
public record KakaoProperties(
		@NotBlank String clientId,
		@NotBlank String redirectUri,
		String clientSecret
) {
}
