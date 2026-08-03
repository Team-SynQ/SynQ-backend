package com.synq.backend.domain.ai.client.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synq.backend.domain.ai.summary.domain.GeneratedSummary;
import com.synq.backend.domain.ai.summary.domain.GeneratedPersonalSummary;
import com.synq.backend.domain.ai.summary.domain.PersonalSummaryAiClient;
import com.synq.backend.domain.ai.summary.domain.PersonalSummaryTarget;
import com.synq.backend.domain.ai.summary.domain.SummaryContext;
import com.synq.backend.domain.ai.summary.domain.SummaryAiClient;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "ai.summary", name = "client", havingValue = "openai")
public class OpenAiSummaryClient implements SummaryAiClient, PersonalSummaryAiClient {

	private static final Map<String, Object> SUMMARY_SCHEMA = Map.of(
			"type", "object",
			"additionalProperties", false,
			"properties", Map.of(
					"oneLineSummary", stringSchema(),
					"keyTopics", stringArraySchema(),
					"discussionSections", discussionSectionArraySchema(),
					"decisions", stringArraySchema(),
					"tentativeDirections", stringArraySchema(),
					"confirmationItems", stringArraySchema()
			),
			"required", List.of(
					"oneLineSummary", "keyTopics", "discussionSections", "decisions",
					"tentativeDirections", "confirmationItems"
			)
	);
	private static final Map<String, Object> PERSONAL_SUMMARY_SCHEMA = Map.of(
			"type", "object",
			"additionalProperties", false,
			"properties", Map.of(
					"personalSummary", stringSchema(),
					"keyPoints", stringArraySchema(),
					"myActionItems", stringArraySchema(),
					"followUpQuestions", stringArraySchema()
			),
			"required", List.of("personalSummary", "keyPoints", "myActionItems", "followUpQuestions")
	);

	private final OpenAiClient openAiClient;
	private final ObjectMapper objectMapper;

	public OpenAiSummaryClient(OpenAiClient openAiClient, ObjectMapper objectMapper) {
		this.openAiClient = openAiClient;
		this.objectMapper = objectMapper;
	}

	@Override
	public GeneratedPersonalSummary generate(
			SummaryContext context,
			GeneratedSummary overallSummary,
			PersonalSummaryTarget target
	) {
		String response = openAiClient.createStructuredText(
				createPersonalPrompt(context, overallSummary, target),
				"personal_meeting_summary",
				PERSONAL_SUMMARY_SCHEMA
		);
		try {
			return objectMapper.readValue(response, GeneratedPersonalSummary.class);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("OpenAI 개인 요약 응답을 읽을 수 없습니다.", e);
		}
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

	private static Map<String, Object> discussionSectionArraySchema() {
		return Map.of(
				"type", "array",
				"items", Map.of(
						"type", "object",
						"additionalProperties", false,
						"properties", Map.of(
								"title", stringSchema(),
								"details", stringArraySchema()
						),
						"required", List.of("title", "details")
				)
		);
	}

	private String createPrompt(SummaryContext context) {
		// 실제 형식 보장은 Responses API의 JSON Schema가 맡고, 프롬프트는 내용 품질에만 집중한다.
		return """
				당신은 회의록 요약 도우미입니다. 아래 정보를 바탕으로 회의 결과를 한국어로 정리하세요.
				회의에 없는 사실, 담당자, 결정 사항은 추측하지 마세요. 해당 내용이 없으면 빈 배열로 두세요.
				oneLineSummary는 회의 핵심을 한 문장으로 작성하세요.
				discussionSections는 주요 논의 주제별로 묶고, details에는 각 주제의 구체적인 논의 내용을 작성하세요.
				tentativeDirections에는 논의되었지만 아직 확정되지 않은 방향만 작성하세요.
				confirmationItems에는 다음 회의 또는 후속 작업에서 확인·결정해야 할 항목만 작성하세요.

				[회의 전사]
				%s

				[참고자료]
				%s
				""".formatted(
				context.transcript(),
				String.join("\n", context.referenceContexts())
		);
	}

	private String createPersonalPrompt(
			SummaryContext context,
			GeneratedSummary overallSummary,
			PersonalSummaryTarget target
	) {
		return """
				당신은 회의 참여자를 위한 개인 회의록 요약 도우미입니다.
				전체 전사와 구조화된 전체 정리를 바탕으로 아래 사용자의 역할과 관심 관점에 맞춰 한국어로 정리하세요.
				화자 식별 정보가 없으므로 사용자가 직접 말했거나 업무를 약속했다고 단정하지 마세요.
				담당자가 전사에서 명확히 확인되지 않는 액션 아이템은 개인 업무로 지정하지 마세요.

				[사용자 역할]
				%s

				[관심 관점]
				%s

				[전체 정리]
				%s

				[회의 전사]
				%s
				""".formatted(
				target.roleDescription(),
				String.join(", ", target.perspectives()),
				formatOverallSummary(overallSummary),
				context.transcript()
		);
	}

	private String formatOverallSummary(GeneratedSummary summary) {
		String sections = summary.discussionSections().stream()
				.map(section -> "- " + section.title() + ": " + String.join(" / ", section.details()))
				.collect(Collectors.joining("\n"));
		return """
				한 줄 요약: %s
				핵심 키워드: %s
				주요 논의:
				%s
				결정된 내용: %s
				논의된 방향: %s
				확인 필요 내용: %s
				""".formatted(
				summary.oneLineSummary(),
				String.join(", ", summary.keyTopics()),
				sections.isBlank() ? "(없음)" : sections,
				String.join(" / ", summary.decisions()),
				String.join(" / ", summary.tentativeDirections()),
				String.join(" / ", summary.confirmationItems())
		);
	}
}
