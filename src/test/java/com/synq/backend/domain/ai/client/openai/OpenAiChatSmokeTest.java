package com.synq.backend.domain.ai.client.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synq.backend.domain.ai.assistant.domain.AiChatContext;
import com.synq.backend.domain.ai.assistant.domain.AiChatPrompt;
import com.synq.backend.domain.ai.assistant.domain.AiChatReference;
import com.synq.backend.domain.ai.assistant.domain.AiChatSource;
import com.synq.backend.domain.ai.assistant.domain.AiChatTranscript;
import com.synq.backend.domain.ai.context.domain.LiveContextSnapshot;
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
 * 실제 OpenAI로 Chat 응답과 초기 추천 질문을 확인하는 선택적 smoke test다.
 */
class OpenAiChatSmokeTest {

	@Test
	void callsOpenAiForChatAndWelcome() {
		Map<String, String> localEnv = loadDotEnv();
		assumeTrue("true".equalsIgnoreCase(valueOf("RUN_OPENAI_CHAT_SMOKE_TEST", localEnv)),
				"RUN_OPENAI_CHAT_SMOKE_TEST=true 일 때만 실제 OpenAI API를 호출합니다.");

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
		OpenAiChatClient client = new OpenAiChatClient(
				new OpenAiClient(restClient, properties), new ObjectMapper());

		var welcome = client.generateWelcome(context());
		var result = client.generate(new AiChatPrompt("이번 주 우선순위는 무엇인가요?", context()));

		System.out.println("OpenAI chat welcome = " + welcome);
		System.out.println("OpenAI chat response = " + result);
		assertThat(welcome.welcomeMessage()).isNotBlank();
		assertThat(welcome.suggestedQuestions()).isNotEmpty();
		assertThat(result.answer()).isNotBlank();
	}

	private AiChatContext context() {
		return new AiChatContext(
				1L,
				2L,
				"DEV_TECH - 백엔드",
				List.of("TECH_RISK", "SCHEDULE"),
				new LiveContextSnapshot(
						"온보딩 개선을 이번 주 우선순위로 두기로 논의했습니다.",
						"온보딩 개선 일정",
						List.of("온보딩 개선을 먼저 진행한다."),
						List.of("API 초안 작성"),
						List.of("QA 기간을 어떻게 확보할지 확인한다.")
				),
				List.of(new AiChatTranscript(11L, null, "온보딩 개선을 이번 주에 먼저 진행합시다.")),
				List.of(),
				List.of(new AiChatReference(7L, 70L, "PRD에는 온보딩 핵심 화면 개선이 우선 과제로 정리되어 있습니다.")),
				List.of(
						new AiChatSource("TRANSCRIPT_SEGMENT", 11L, "현재 회의 발화 0"),
						new AiChatSource("REFERENCE_MATERIAL", 7L, "참고자료 7")
				)
		);
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
		} catch (IOException exception) {
			throw new IllegalStateException(".env 파일을 읽지 못했습니다.", exception);
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
