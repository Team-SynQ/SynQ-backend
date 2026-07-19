package com.synq.backend.domain.ai.client.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synq.backend.domain.ai.summary.domain.GeneratedSummary;
import com.synq.backend.domain.ai.summary.domain.SummaryContext;
import com.synq.backend.domain.ai.summary.domain.SummaryAiClient;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "ai.summary", name = "client", havingValue = "openai")
public class OpenAiSummaryClient implements SummaryAiClient {

	private static final Map<String, Object> SUMMARY_SCHEMA = Map.of(
			"type", "object",
			"additionalProperties", false,
			"properties", Map.of(
					"overallSummary", stringSchema(),
					"keyTopics", stringArraySchema(),
					"decisions", stringArraySchema(),
					"actionItems", stringArraySchema(),
					"openQuestions", stringArraySchema()
			),
			"required", List.of("overallSummary", "keyTopics", "decisions", "actionItems", "openQuestions")
	);

	private final OpenAiClient openAiClient;
	private final ObjectMapper objectMapper;

	public OpenAiSummaryClient(OpenAiClient openAiClient, ObjectMapper objectMapper) {
		this.openAiClient = openAiClient;
		this.objectMapper = objectMapper;
	}

	@Override
	public GeneratedSummary generate(SummaryContext context) {
		String response = openAiClient.createStructuredText(
				createPrompt(context),
				"meeting_summary",
				SUMMARY_SCHEMA
		);
		try {
			// SummaryAiClient의 반환 형식을 고정해 Controller와 저장소가 제공자별 차이를 몰라도 되게 한다.
			return objectMapper.readValue(response, GeneratedSummary.class);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("OpenAI 요약 응답을 읽을 수 없습니다.", e);
		}
	}

	private static Map<String, Object> stringSchema() {
		return Map.of("type", "string");
	}

	private static Map<String, Object> stringArraySchema() {
		return Map.of(
				"type", "array",
				"items", stringSchema()
		);
	}

	private String createPrompt(SummaryContext context) {
		// 실제 형식 보장은 Responses API의 JSON Schema가 맡고, 프롬프트는 내용 품질에만 집중한다.
		return """
				당신은 회의록 요약 도우미입니다. 아래 정보를 바탕으로 회의 결과를 한국어로 정리하세요.
				결정되지 않은 항목은 빈 배열로 두고, 회의에 없는 사실은 추측하지 마세요.

				[회의 전사]
				%s

				[회의 누적 맥락]
				%s

				[참고자료]
				%s
				""".formatted(
				context.transcript(),
				context.rollingSummary(),
				String.join("\n", context.referenceContexts())
		);
	}
}
