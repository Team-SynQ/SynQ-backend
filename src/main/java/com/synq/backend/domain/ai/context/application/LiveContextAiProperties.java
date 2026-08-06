package com.synq.backend.domain.ai.context.application;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ai.live-context")
public record LiveContextAiProperties(
		@NotBlank String model,
		@NotBlank String reasoningEffort,
		@Min(1) @Max(100_000) int maxOutputTokens
) {
}
