package com.synq.backend.domain.ai.client.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class OpenAiClientTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void extractsOutputTextField() throws Exception {
		JsonNode response = objectMapper.readTree("""
				{
				  "output_text": "회의 내용을 정리했습니다."
				}
				""");

		String result = OpenAiClient.extractOutputText(response);

		assertThat(result).isEqualTo("회의 내용을 정리했습니다.");
	}

	@Test
	void extractsNestedOutputContentText() throws Exception {
		JsonNode response = objectMapper.readTree("""
				{
				  "output": [
				    {
				      "type": "message",
				      "content": [
				        {
				          "type": "output_text",
				          "text": "첫 번째 문장"
				        },
				        {
				          "type": "output_text",
				          "text": "두 번째 문장"
				        }
				      ]
				    }
				  ]
				}
				""");

		String result = OpenAiClient.extractOutputText(response);

		assertThat(result).isEqualTo("첫 번째 문장\n두 번째 문장");
	}

	@Test
	void throwsExceptionWhenApiKeyMissing() {
		OpenAiProperties properties = new OpenAiProperties(
				"",
				"https://api.openai.com/v1",
				"gpt-5.4-nano",
				Duration.ofSeconds(30)
		);
		OpenAiClient client = new OpenAiClient(RestClient.builder().build(), properties);

		assertThatThrownBy(() -> client.createText("회의를 요약해줘."))
				.isInstanceOf(OpenAiException.class)
				.extracting("code")
				.isEqualTo(OpenAiErrorCode.API_KEY_MISSING);
	}
}
