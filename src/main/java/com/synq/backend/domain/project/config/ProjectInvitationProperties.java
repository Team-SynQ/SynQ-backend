package com.synq.backend.domain.project.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "project.invitation")
public record ProjectInvitationProperties(
		@NotBlank String frontendBaseUrl,
		@Positive long expirationDays
) {
}
