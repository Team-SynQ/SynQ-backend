package com.synq.backend.domain.ai.client.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synq.backend.domain.ai.assistant.domain.HintInput;
import com.synq.backend.domain.ai.assistant.domain.HintResult;
import com.synq.backend.domain.ai.context.domain.LiveContextSnapshot;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpenAiHintClientTest {

	@Mock
	OpenAiClient openAiClient;

	@Test
	void 구조화_응답을_HintResult_로_파싱한다() {
		String json = """
				{"meaning":"의미다","myImpact":"영향이다","teamQuestion":"질문이다"}""";
		given(openAiClient.createStructuredText(any(), eq("three_hint"), any())).willReturn(json);

		OpenAiHintClient client = new OpenAiHintClient(openAiClient, new ObjectMapper());
		HintInput input = new HintInput("발화", List.of(), List.of(), "PM", "속도 우선",
				LiveContextSnapshot.empty(), List.of());

		HintResult result = client.generate(input);

		assertThat(result.meaning()).isEqualTo("의미다");
		assertThat(result.myImpact()).isEqualTo("영향이다");
		assertThat(result.teamQuestion()).isEqualTo("질문이다");
	}
}
