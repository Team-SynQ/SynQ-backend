package com.synq.backend.domain.ai.client.openai;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(
		String apiKey,
		String baseUrl,
		String model,
		Duration timeout
) {
}
