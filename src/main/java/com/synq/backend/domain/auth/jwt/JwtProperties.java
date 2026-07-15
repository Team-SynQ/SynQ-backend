package com.synq.backend.domain.auth.jwt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
		@NotBlank String secret,
		@Positive long accessTokenExpirationMinutes,
		@Positive long refreshTokenExpirationDays
) {
}
