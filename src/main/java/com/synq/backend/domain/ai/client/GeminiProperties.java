package com.synq.backend.domain.ai.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.yml 의 gemini.* 를 바인딩한다. */
@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(String apiKey, String baseUrl, Embedding embedding) {

	public record Embedding(
			String model,
			int dimensions,
			int batchSize,
			int maxAttempts,
			long initialBackoffMillis,
			long backoffMultiplier
	) {
	}
}
