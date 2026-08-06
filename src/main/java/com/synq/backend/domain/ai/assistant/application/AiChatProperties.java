package com.synq.backend.domain.ai.assistant.application;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ai.chat")
public record AiChatProperties(
		@NotBlank String model,
		@NotBlank String reasoningEffort,
		@Min(1) @Max(100_000) int maxOutputTokens,
		@Min(1) @Max(30) int recentTranscriptLimit,
		@Min(0) @Max(20) int linkedSegmentWindow,
		@Min(0) @Max(20) int recentTurnLimit,
		@Min(1) @Max(10) int ragTopK,
		@DecimalMin("-1.0") @DecimalMax("1.0") double ragMinSimilarity
) {
}
