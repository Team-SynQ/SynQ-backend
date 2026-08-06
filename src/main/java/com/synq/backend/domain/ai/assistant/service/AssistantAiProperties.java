package com.synq.backend.domain.ai.assistant.service;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ai.assistant")
public record AssistantAiProperties(
		@NotBlank String model,
		@NotBlank String reasoningEffort,
		@Min(1) @Max(100_000) int maxOutputTokens
) {
}
