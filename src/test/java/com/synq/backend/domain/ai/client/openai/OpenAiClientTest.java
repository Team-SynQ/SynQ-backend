package com.synq.backend.domain.ai.client.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
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
		OpenAiClient client = new OpenAiClient(
				RestClient.builder().build(),
				properties("", "gpt-5.4-nano"));

		assertThatThrownBy(() -> client.createText("회의를 요약해줘."))
				.isInstanceOf(OpenAiException.class)
				.extracting("code")
				.isEqualTo(OpenAiErrorCode.API_KEY_MISSING);
	}

	@Test
	void throwsExceptionWhenResponseIsNull() {
		assertThatThrownBy(() -> OpenAiClient.extractOutputText(null))
				.isInstanceOf(OpenAiException.class)
				.extracting("code")
				.isEqualTo(OpenAiErrorCode.INVALID_RESPONSE);
	}

	@Test
	void throwsExceptionWhenInputIsBlank() {
		OpenAiClient client = new OpenAiClient(
				RestClient.builder().build(),
				properties("test-key", "gpt-5.4-nano"));

		assertThatThrownBy(() -> client.createText(" "))
				.isInstanceOf(OpenAiException.class)
				.extracting("code")
				.isEqualTo(OpenAiErrorCode.EMPTY_INPUT);
	}

	@Test
	void throwsExceptionWhenModelIsBlank() {
		OpenAiClient client = new OpenAiClient(
				RestClient.builder().build(),
				properties("test-key", ""));

		assertThatThrownBy(() -> client.createText("회의를 요약해줘."))
				.isInstanceOf(OpenAiException.class)
				.extracting("code")
				.isEqualTo(OpenAiErrorCode.MODEL_MISSING);
	}

	@Test
	void wrapsRestClientException() {
		RestClient restClient = RestClient.builder()
				.requestFactory((uri, httpMethod) -> {
					throw new ResourceAccessException("OpenAI 연결 실패");
				})
				.build();
		OpenAiClient client = new OpenAiClient(
				restClient,
				properties("test-key", "gpt-5.4-nano"));

		assertThatThrownBy(() -> client.createText("회의를 요약해줘."))
				.isInstanceOf(OpenAiException.class)
				.extracting("code")
				.isEqualTo(OpenAiErrorCode.REQUEST_FAILED);
	}

	private OpenAiProperties properties(String apiKey, String model) {
		return new OpenAiProperties(
				apiKey,
				"https://api.openai.com/v1",
				model,
				Duration.ofSeconds(30)
		);
	}
}
