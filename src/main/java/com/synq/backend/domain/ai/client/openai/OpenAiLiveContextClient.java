package com.synq.backend.domain.ai.client.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synq.backend.domain.ai.context.domain.LiveContextAiClient;
import com.synq.backend.domain.ai.context.domain.LiveContextResult;
import com.synq.backend.domain.ai.context.domain.LiveContextSnapshot;
import com.synq.backend.domain.transcript.event.TranscriptFinalizedEvent;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 기존 회의 맥락과 새 확정 전사를 OpenAI Responses API에 전달해 최신 맥락을 생성한다.
 */
@Component
@ConditionalOnProperty(prefix = "ai.live-context", name = "client", havingValue = "openai")
public class OpenAiLiveContextClient implements LiveContextAiClient {

	private static final Map<String, Object> LIVE_CONTEXT_SCHEMA = Map.of(
			"type", "object",
			"additionalProperties", false,
			"properties", Map.of(
					"rollingSummary", stringSchema(),
					"currentTopic", nullableStringSchema(),
					"decisions", stringArraySchema(),
					"actionItems", stringArraySchema(),
					"openQuestions", stringArraySchema()
			),
			"required", List.of("rollingSummary", "currentTopic", "decisions", "actionItems", "openQuestions")
	);

	private final OpenAiClient openAiClient;
	private final ObjectMapper objectMapper;

	public OpenAiLiveContextClient(OpenAiClient openAiClient, ObjectMapper objectMapper) {
		this.openAiClient = openAiClient;
		this.objectMapper = objectMapper;
	}

	@Override
	public LiveContextResult refresh(LiveContextSnapshot previousContext, TranscriptFinalizedEvent event) {
		String response = openAiClient.createStructuredText(
				createPrompt(previousContext, event),
				"meeting_live_context",
				LIVE_CONTEXT_SCHEMA
		);
		try {
			return objectMapper.readValue(response, LiveContextResult.class);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("OpenAI 회의 맥락 응답을 읽을 수 없습니다.", e);
		}
	}

	private static Map<String, Object> stringSchema() {
		return Map.of("type", "string");
	}

	private static Map<String, Object> nullableStringSchema() {
		return Map.of("type", List.of("string", "null"));
	}

	private static Map<String, Object> stringArraySchema() {
		return Map.of("type", "array", "items", stringSchema());
	}

	private String createPrompt(LiveContextSnapshot previousContext, TranscriptFinalizedEvent event) {
		return """
				당신은 실시간 회의 맥락 관리 도우미입니다. 기존 회의 맥락과 새로 확정된 발화를 반영해 최신 상태를 한국어로 갱신하세요.
				이전에 결정된 사항은 유지하되, 새 발화가 이를 명시적으로 변경한 경우에만 수정하세요.
				회의에 없는 사실을 추측하지 말고, 아직 답이 없는 의문은 openQuestions에 남기세요.

				[기존 누적 요약]
				%s

				[기존 현재 주제]
				%s

				[기존 결정 사항]
				%s

				[기존 액션 아이템]
				%s

				[기존 미해결 질문]
				%s

				[새 확정 발화]
				%s
				""".formatted(
				previousContext.rollingSummary(),
				previousContext.currentTopic(),
				String.join("\n", previousContext.decisions()),
				String.join("\n", previousContext.actionItems()),
				String.join("\n", previousContext.openQuestions()),
				event.content()
		);
	}
}
