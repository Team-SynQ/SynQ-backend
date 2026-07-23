package com.synq.backend.domain.ai.client.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synq.backend.domain.ai.context.domain.LiveContextSnapshot;
import com.synq.backend.domain.transcript.event.TranscriptFinalizedEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * 실제 OpenAI Responses API로 Live Context 구조화 응답을 한 번 확인하는 선택적 smoke test다.
 */
class OpenAiLiveContextSmokeTest {

	@Test
	void callsOpenAiForLiveContext() {
		Map<String, String> localEnv = loadDotEnv();
		assumeTrue("true".equalsIgnoreCase(valueOf("RUN_OPENAI_LIVE_CONTEXT_SMOKE_TEST", localEnv)),
				"RUN_OPENAI_LIVE_CONTEXT_SMOKE_TEST=true 일 때만 실제 OpenAI API를 호출합니다.");

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
		OpenAiLiveContextClient client = new OpenAiLiveContextClient(
				new OpenAiClient(restClient, properties), new ObjectMapper());

		var result = client.refresh(
				new LiveContextSnapshot(
						"회의 후 AI 기능의 구현 순서를 논의했다.",
						"AI 기능 구현",
						List.of("Live Context를 먼저 구현한다."),
						List.of(),
						List.of("실제 STT 연동 시점을 정한다.")
				),
				new TranscriptFinalizedEvent(
						1L, 1L, 0, 0, 3000,
						"다음 스프린트에는 전사 이벤트를 연결하고, 회의 중 힌트 기능도 붙이겠습니다.",
						null
				)
		);

		System.out.println("OpenAI live-context smoke response = " + result);
		assertThat(result.rollingSummary()).isNotBlank();
		assertThat(result.currentTopic()).isNotBlank();
	}

	private String valueOrDefault(String name, String defaultValue, Map<String, String> localEnv) {
		String value = valueOf(name, localEnv);
		return value == null || value.isBlank() ? defaultValue : value;
	}

	private String valueOf(String name, Map<String, String> localEnv) {
		String value = System.getenv(name);
		return value == null || value.isBlank() ? localEnv.get(name) : value;
	}

	private Map<String, String> loadDotEnv() {
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
