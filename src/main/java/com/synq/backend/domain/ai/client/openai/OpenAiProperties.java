package com.synq.backend.domain.ai.client.openai;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "openai")
@Validated
@ConditionalOnProperty(prefix = "ai.summary", name = "client", havingValue = "openai")
public record OpenAiProperties(
		@NotBlank String apiKey,
		@NotBlank String baseUrl,
		@NotBlank String model,
		@NotNull Duration timeout
) {

	@AssertTrue(message = "OpenAI timeout은 양수여야 합니다.")
	public boolean isTimeoutPositive() {
		return timeout != null && !timeout.isZero() && !timeout.isNegative();
	}
}
