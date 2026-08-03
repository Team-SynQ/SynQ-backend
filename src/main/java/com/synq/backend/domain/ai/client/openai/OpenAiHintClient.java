package com.synq.backend.domain.ai.client.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synq.backend.domain.ai.assistant.domain.HintAiClient;
import com.synq.backend.domain.ai.assistant.domain.HintInput;
import com.synq.backend.domain.ai.assistant.domain.HintResult;
import com.synq.backend.domain.ai.prompt.HintPromptFactory;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "ai.assistant", name = "client", havingValue = "openai")
public class OpenAiHintClient implements HintAiClient {

	private static Map<String, Object> stringSchema() {
		return Map.of("type", "string");
	}

	private static final Map<String, Object> HINT_SCHEMA = Map.of(
			"type", "object",
			"additionalProperties", false,
			"properties", Map.of(
					"meaning", stringSchema(),
					"myImpact", stringSchema(),
					"teamQuestion", stringSchema()
			),
			"required", List.of("meaning", "myImpact", "teamQuestion")
	);

	private final OpenAiClient openAiClient;
	private final ObjectMapper objectMapper;
	private final HintPromptFactory promptFactory;

	public OpenAiHintClient(
			OpenAiClient openAiClient,
			ObjectMapper objectMapper,
			HintPromptFactory promptFactory
	) {
		this.openAiClient = openAiClient;
		this.objectMapper = objectMapper;
		this.promptFactory = promptFactory;
	}

	@Override
	public HintResult generate(HintInput input) {
		String response = openAiClient.createStructuredText(
				promptFactory.create(input), "three_hint", HINT_SCHEMA);
		try {
			return objectMapper.readValue(response, HintResult.class);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("OpenAI 3-hint 응답을 읽을 수 없습니다.", e);
		}
	}
}
