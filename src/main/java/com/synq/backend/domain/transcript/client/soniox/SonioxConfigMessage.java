package com.synq.backend.domain.transcript.client.soniox;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Soniox 연결 직후 가장 먼저 보내는 설정 프레임.
 * 필드명/모델명은 Soniox WS 문서 기준이며, 실제 연동에서 확정해야 한다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SonioxConfigMessage(
		@JsonProperty("api_key") String apiKey,
		@JsonProperty("model") String model,
		@JsonProperty("audio_format") String audioFormat,
		@JsonProperty("language_hints") List<String> languageHints,
		// 세그먼트 확정의 주 경로인 <end> 토큰이 이 옵션에서 나온다.
		@JsonProperty("enable_endpoint_detection") Boolean enableEndpointDetection
) {
}
