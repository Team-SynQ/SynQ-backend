package com.synq.backend.domain.ai.client.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class OpenAiClientSmokeTest {

	@Test
	void callsOpenAiResponsesApi() {
		Map<String, String> localEnv = loadDotEnv(false);
		assumeTrue("true".equalsIgnoreCase(valueOf("RUN_OPENAI_SMOKE_TEST", localEnv)),
				"RUN_OPENAI_SMOKE_TEST=true 일 때만 실제 OpenAI API를 호출합니다.");

		localEnv = loadDotEnv(true);
		String apiKey = valueOf("OPENAI_API_KEY", localEnv);
		assumeTrue(apiKey != null && !apiKey.isBlank(),
				"OPENAI_API_KEY가 설정되어 있어야 실제 OpenAI API를 호출합니다.");

		OpenAiProperties properties = new OpenAiProperties(
				apiKey,
				valueOrDefault("OPENAI_BASE_URL", "https://api.openai.com/v1", localEnv),
				valueOrDefault("OPENAI_MODEL", "gpt-5.4-nano", localEnv),
				Duration.ofSeconds(30)
		);
		RestClient restClient = new OpenAiClientConfig()
				.openAiRestClient(RestClient.builder(), properties);
		OpenAiClient client = new OpenAiClient(restClient, properties);

		String result = client.createText("OpenAI 연결 테스트입니다. OK라고만 답해주세요.");

		System.out.println("OpenAI smoke response = " + result);
		assertThat(result).isNotBlank();
	}

	private String valueOrDefault(String name, String defaultValue, Map<String, String> localEnv) {
		String value = valueOf(name, localEnv);
		return value == null || value.isBlank() ? defaultValue : value;
	}

	private String valueOf(String name, Map<String, String> localEnv) {
		String value = System.getenv(name);
		return value == null || value.isBlank() ? localEnv.get(name) : value;
	}

	private Map<String, String> loadDotEnv(boolean failOnError) {
		Path path = Path.of(".env");
		if (!Files.exists(path)) {
			return Map.of();
		}

		try {
			Map<String, String> values = new HashMap<>();
			for (String line : Files.readAllLines(path)) {
				String trimmed = line.trim();
				if (trimmed.isBlank() || trimmed.startsWith("#") || !trimmed.contains("=")) {
					continue;
				}

				String[] parts = trimmed.split("=", 2);
				values.put(parts[0].trim(), unquote(parts[1].trim()));
			}
			return values;
		} catch (IOException e) {
			if (!failOnError) {
				return Map.of();
			}
			throw new IllegalStateException(".env 파일을 읽지 못했습니다.", e);
		}
	}

	private String unquote(String value) {
		if (value.length() >= 2
				&& ((value.startsWith("\"") && value.endsWith("\""))
				|| (value.startsWith("'") && value.endsWith("'")))) {
			return value.substring(1, value.length() - 1);
		}
		return value;
	}
}
