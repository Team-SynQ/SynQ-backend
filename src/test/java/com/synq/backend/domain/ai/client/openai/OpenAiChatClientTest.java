package com.synq.backend.domain.ai.client.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synq.backend.domain.ai.assistant.application.AiChatProperties;
import com.synq.backend.domain.ai.assistant.domain.AiChatContext;
import com.synq.backend.domain.ai.assistant.domain.AiChatPrompt;
import com.synq.backend.domain.ai.assistant.domain.AiChatReference;
import com.synq.backend.domain.ai.rag.search.ChunkSource;
import com.synq.backend.domain.ai.assistant.domain.AiChatSource;
import com.synq.backend.domain.ai.assistant.domain.AiChatTranscript;
import com.synq.backend.domain.ai.context.domain.LiveContextSnapshot;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class OpenAiChatClientTest {

	private final OpenAiClient openAiClient = Mockito.mock(OpenAiClient.class);
	private final OpenAiChatClient client = new OpenAiChatClient(openAiClient, new ObjectMapper(), properties());

	@Test
	void Chat_응답은_허용된_출처만_저장하고_질문_맥락을_프롬프트에_포함한다() {
		when(openAiClient.createStructuredText(anyString(), eq("meeting_ai_chat"), anyMap(), Mockito.any()))
				.thenReturn("""
						{
						  "answer": "온보딩 개선이 이번 주 우선순위입니다.",
						  "sourceKeys": ["TRANSCRIPT_SEGMENT:11", "REFERENCE_MATERIAL:7", "UNKNOWN:1"],
						  "suggestedQuestions": ["완료 기준은 무엇인가요?", "일정 위험은 무엇인가요?"]
						}
						""");

		var result = client.generate(new AiChatPrompt("이번 주 우선순위가 뭐야?", context()));

		assertThat(result.answer()).contains("온보딩 개선");
		assertThat(result.sources()).extracting(AiChatSource::type)
				.containsExactly("TRANSCRIPT_SEGMENT", "REFERENCE_MATERIAL");
		assertThat(result.suggestedQuestions())
				.containsExactly("완료 기준은 무엇인가요?", "일정 위험은 무엇인가요?");

		ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
		verify(openAiClient).createStructuredText(prompt.capture(), eq("meeting_ai_chat"), anyMap(),
				eq(new OpenAiGenerationOptions("chat-model", "low", 2_000)));
		assertThat(prompt.getValue()).contains(
				"개발·기술 - 백엔드",
				"기술 리스크",
				"온보딩 개선은 이번 주 우선순위입니다.",
				"이번 주 우선순위가 뭐야?",
				"REFERENCE_MATERIAL:7"
		);
		assertThat(prompt.getValue()).doesNotContain("DEV_TECH", "TECH_RISK");
	}

	@Test
	void 최초_진입_응답은_안내와_추천_질문을_읽는다() {
		when(openAiClient.createStructuredText(anyString(), eq("meeting_ai_chat_welcome"), anyMap()))
				.thenReturn("""
						{
						  "welcomeMessage": "회의가 시작되었습니다.",
						  "suggestedQuestions": ["현재 결정된 사항은 무엇인가요?", "제가 확인할 다음 작업은 무엇인가요?"]
						}
						""");

		var welcome = client.generateWelcome(context());

		assertThat(welcome.welcomeMessage()).isEqualTo("회의가 시작되었습니다.");
		assertThat(welcome.suggestedQuestions()).hasSize(2);
		verify(openAiClient).createStructuredText(anyString(), eq("meeting_ai_chat_welcome"), anyMap());
	}

	@Test
	void 최초_진입_프롬프트도_역할과_관점을_한글_라벨로_넣는다() {
		when(openAiClient.createStructuredText(anyString(), eq("meeting_ai_chat_welcome"), anyMap()))
				.thenReturn("""
						{
						  "welcomeMessage": "회의가 시작되었습니다.",
						  "suggestedQuestions": ["현재 결정된 사항은 무엇인가요?", "제가 확인할 다음 작업은 무엇인가요?"]
						}
						""");

		client.generateWelcome(context());

		ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
		verify(openAiClient).createStructuredText(
				prompt.capture(), eq("meeting_ai_chat_welcome"), anyMap());
		assertThat(prompt.getValue()).contains("개발·기술 - 백엔드", "기술 리스크");
		assertThat(prompt.getValue()).doesNotContain("DEV_TECH", "TECH_RISK");
	}

	@Test
	void 역할_프로필이_없으면_미입력으로_표기한다() {
		when(openAiClient.createStructuredText(anyString(), eq("meeting_ai_chat"), anyMap(), Mockito.any()))
				.thenReturn("""
						{
						  "answer": "확인이 필요합니다.",
						  "sourceKeys": [],
						  "suggestedQuestions": ["완료 기준은 무엇인가요?", "일정 위험은 무엇인가요?"]
						}
						""");

		client.generate(new AiChatPrompt("이번 주 우선순위가 뭐야?", emptyProfileContext()));

		ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
		verify(openAiClient).createStructuredText(prompt.capture(), eq("meeting_ai_chat"), anyMap(),
				Mockito.any());
		assertThat(prompt.getValue()).contains("역할: (미입력)", "관심 관점: (미입력)");
	}

	private AiChatContext emptyProfileContext() {
		return new AiChatContext(
				1L,
				2L,
				"",
				"",
				List.of(),
				new LiveContextSnapshot("", null, List.of(), List.of(), List.of()),
				List.of(),
				List.of(),
				List.of(),
				List.of()
		);
	}

	private AiChatContext context() {
		return new AiChatContext(
				1L,
				2L,
				"DEV_TECH",
				"백엔드",
				List.of("TECH_RISK"),
				new LiveContextSnapshot("온보딩 개선은 이번 주 우선순위입니다.", "일정", List.of(), List.of(), List.of()),
				List.of(new AiChatTranscript(11L, null, "온보딩 개선을 먼저 진행합시다.")),
				List.of(),
				List.of(new AiChatReference(ChunkSource.REFERENCE_MATERIAL, 7L, 70L, "PRD의 온보딩 개선 계획")),
				List.of(
						new AiChatSource("TRANSCRIPT_SEGMENT", 11L, "현재 회의 발화 0"),
						new AiChatSource("REFERENCE_MATERIAL", 7L, "참고자료 7")
				)
		);
	}

	private AiChatProperties properties() {
		return new AiChatProperties("chat-model", "low", 2_000, 12, 2, 5, 5, 0.5);
	}
}
