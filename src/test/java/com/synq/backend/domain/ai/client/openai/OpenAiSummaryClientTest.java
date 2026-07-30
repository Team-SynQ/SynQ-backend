package com.synq.backend.domain.ai.client.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synq.backend.domain.ai.summary.domain.GeneratedSummary;
import com.synq.backend.domain.ai.summary.domain.PersonalSummaryTarget;
import com.synq.backend.domain.ai.summary.domain.SummaryContext;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class OpenAiSummaryClientTest {

	private final OpenAiClient openAiClient = Mockito.mock(OpenAiClient.class);
	private final OpenAiSummaryClient client = new OpenAiSummaryClient(openAiClient, new ObjectMapper());

	@Test
	void 전체_요약은_전체_전사와_참고자료만_프롬프트에_포함한다() {
		when(openAiClient.createStructuredText(
				Mockito.anyString(), eq("meeting_summary"), anyMap()))
				.thenReturn("""
						{
						  "overallSummary": "전체 요약",
						  "keyTopics": [],
						  "decisions": [],
						  "actionItems": [],
						  "openQuestions": []
						}
						""");

		client.generate(new SummaryContext(1L, "실제 전체 전사", List.of("관련 참고자료")));

		ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
		verify(openAiClient).createStructuredText(prompt.capture(), eq("meeting_summary"), anyMap());
		assertThat(prompt.getValue())
				.contains("실제 전체 전사", "관련 참고자료")
				.doesNotContain("회의 누적 맥락");
	}

	@Test
	void 개인_요약은_역할과_관점을_반영하고_발화를_단정하지_않는다() {
		when(openAiClient.createStructuredText(
				Mockito.anyString(), eq("personal_meeting_summary"), anyMap()))
				.thenReturn("""
						{
						  "personalSummary": "개인 요약",
						  "keyPoints": [],
						  "myActionItems": [],
						  "followUpQuestions": []
						}
						""");
		var context = new SummaryContext(1L, "전체 전사", List.of());
		var overall = new GeneratedSummary("전체 요약", List.of(), List.of(), List.of(), List.of());

		client.generate(
				context,
				overall,
				new PersonalSummaryTarget(7L, "DEV_TECH - 백엔드", List.of("TECH_RISK"))
		);

		ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
		verify(openAiClient).createStructuredText(
				prompt.capture(), eq("personal_meeting_summary"), anyMap());
		assertThat(prompt.getValue())
				.contains("DEV_TECH - 백엔드", "TECH_RISK")
				.contains("직접 말했거나 업무를 약속했다고 단정하지 마세요");
	}
}
