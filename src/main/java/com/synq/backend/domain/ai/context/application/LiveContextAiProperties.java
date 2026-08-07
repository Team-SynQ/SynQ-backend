package com.synq.backend.domain.ai.context.application;

import com.synq.backend.domain.ai.client.openai.OpenAiGenerationOptions;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ai.live-context")
public record LiveContextAiProperties(
		@NotBlank String model,
		@NotBlank @Pattern(regexp = OpenAiGenerationOptions.REASONING_EFFORT_PATTERN) String reasoningEffort,
		@Min(1) @Max(100_000) int maxOutputTokens
) {
}
