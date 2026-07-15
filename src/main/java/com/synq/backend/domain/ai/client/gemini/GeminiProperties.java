package com.synq.backend.domain.ai.client.gemini;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * application.yml 의 gemini.* 를 바인딩한다.
 * 잘못된 값(batchSize=0 이면 무한 루프)은 기동 시점에 잡는다.
 */
@Validated
@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
		@NotBlank String apiKey,
		@NotBlank String baseUrl,
		@Valid Embedding embedding
) {

	public record Embedding(
			@NotBlank String model,
			@Positive int dimensions,
			@Positive int batchSize,
			@Positive int maxAttempts,
			@Positive long initialBackoffMillis,
			@Positive long backoffMultiplier
	) {
	}
}
