package com.synq.backend.domain.transcript.client.soniox;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Soniox 실시간 응답 프레임.
 * 스키마 확정 전까지는 SonioxStreamClient 가 원본 JSON 을 그대로 로깅한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SonioxResponse(
		@JsonProperty("tokens") List<SonioxToken> tokens,
		@JsonProperty("finished") Boolean finished,
		@JsonProperty("error_code") String errorCode,
		@JsonProperty("error_message") String errorMessage
) {

	public List<SonioxToken> tokensOrEmpty() {
		return tokens == null ? List.of() : tokens;
	}

	public boolean hasError() {
		return errorCode != null || errorMessage != null;
	}
}
