package com.synq.backend.domain.ai.client.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synq.backend.domain.ai.context.domain.LiveContextSnapshot;
import com.synq.backend.domain.transcript.event.TranscriptFinalizedEvent;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class OpenAiLiveContextClientTest {

	@Test
	void 구조화된_OpenAI_응답을_회의_맥락으로_변환한다() {
		OpenAiClient openAiClient = Mockito.mock(OpenAiClient.class);
		given(openAiClient.createStructuredText(any(), eq("meeting_live_context"), org.mockito.ArgumentMatchers.<Map<String, Object>>any()))
				.willReturn("""
						{
						  "rollingSummary": "회의 후 AI 기능을 먼저 구현하기로 했다.",
						  "currentTopic": "구현 순서",
						  "decisions": ["Live Context를 우선 구현한다."],
						  "actionItems": ["전사 이벤트 계약을 정의한다."],
						  "openQuestions": []
						}
						""");

		OpenAiLiveContextClient client = new OpenAiLiveContextClient(openAiClient, new ObjectMapper());
		var result = client.refresh(
				new LiveContextSnapshot("기존 요약", null, List.of(), List.of(), List.of()),
				new TranscriptFinalizedEvent(1L, 1L, 0, 0, 1000, "확정 전사", null)
		);

		assertThat(result.rollingSummary()).isEqualTo("회의 후 AI 기능을 먼저 구현하기로 했다.");
		assertThat(result.decisions()).containsExactly("Live Context를 우선 구현한다.");
		assertThat(result.openQuestions()).isEmpty();

		ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
		verify(openAiClient).createStructuredText(
				promptCaptor.capture(),
				eq("meeting_live_context"),
				org.mockito.ArgumentMatchers.<Map<String, Object>>any()
		);
		assertThat(promptCaptor.getValue())
				.contains("[기존 현재 주제]\n없음")
				.doesNotContain("null");
	}
}
