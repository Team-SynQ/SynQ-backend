package com.synq.backend.domain.ai.client.openai;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * OpenAI Responses API 호출을 담당하는 클라이언트.
 */
@Component
public class OpenAiClient {

	private final RestClient openAiRestClient;
	private final OpenAiProperties properties;

	public OpenAiClient(
			@Qualifier("openAiRestClient") RestClient openAiRestClient,
			OpenAiProperties properties
	) {
		this.openAiRestClient = openAiRestClient;
		this.properties = properties;
	}

	public String createText(String input) {
		return createText(input, properties.model());
	}

	/**
	 * 프롬프트를 OpenAI에 전달하고 생성된 텍스트를 반환한다.
	 */
	public String createText(String input, String model) {
		validateRequest(input, model);

		Map<String, String> requestBody = Map.of(
				"model", model,
				"input", input
		);

		try {
			JsonNode response = openAiRestClient.post()
					.uri("/responses")
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
					.body(requestBody)
					.retrieve()
					.body(JsonNode.class);

			return extractOutputText(response);
		} catch (OpenAiException e) {
			throw e;
		} catch (RestClientException e) {
			throw new OpenAiException(OpenAiErrorCode.REQUEST_FAILED, e);
		}
	}

	private void validateRequest(String input, String model) {
		if (!StringUtils.hasText(properties.apiKey())) {
			throw new OpenAiException(OpenAiErrorCode.API_KEY_MISSING);
		}
		if (!StringUtils.hasText(input) || !StringUtils.hasText(model)) {
			throw new OpenAiException(OpenAiErrorCode.EMPTY_INPUT);
		}
	}

	/**
	 * 응답 옵션에 따라 텍스트 위치가 달라질 수 있어 두 경로를 모두 확인한다.
	 */
	static String extractOutputText(JsonNode response) {
		if (response == null || response.isNull()) {
			throw new OpenAiException(OpenAiErrorCode.INVALID_RESPONSE);
		}

		JsonNode outputText = response.path("output_text");
		if (outputText.isTextual() && StringUtils.hasText(outputText.asText())) {
			return outputText.asText();
		}

		List<String> texts = new ArrayList<>();
		JsonNode output = response.path("output");
		if (output.isArray()) {
			for (JsonNode outputItem : output) {
				JsonNode content = outputItem.path("content");
				if (!content.isArray()) {
					continue;
				}
				for (JsonNode contentItem : content) {
					JsonNode text = contentItem.path("text");
					if (text.isTextual() && StringUtils.hasText(text.asText())) {
						texts.add(text.asText());
					}
				}
			}
		}

		if (texts.isEmpty()) {
			throw new OpenAiException(OpenAiErrorCode.INVALID_RESPONSE);
		}
		return String.join("\n", texts);
	}
}
