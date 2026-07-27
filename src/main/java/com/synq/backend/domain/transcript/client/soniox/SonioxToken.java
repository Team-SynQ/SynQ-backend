package com.synq.backend.domain.transcript.client.soniox;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Soniox 실시간 응답의 토큰 하나.
 * 필드명은 Soniox WS 문서 기준이며, 실제 응답을 확인하고 확정해야 한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SonioxToken(
		@JsonProperty("text") String text,
		@JsonProperty("start_ms") Integer startMs,
		@JsonProperty("end_ms") Integer endMs,
		@JsonProperty("is_final") Boolean isFinal,
		@JsonProperty("language") String language
) {

	/** 엔드포인트(발화 끝) 표식. 이 토큰을 만나면 버퍼를 확정한다. */
	public static final String END_MARKER = "<end>";
	/** 스트림 종료 표식. 빈 프레임 전송 후 Soniox 가 마지막으로 내려준다. */
	public static final String FIN_MARKER = "<fin>";

	public boolean isEndMarker() {
		return END_MARKER.equals(text);
	}

	public boolean isFinMarker() {
		return FIN_MARKER.equals(text);
	}

	/** 표식 토큰은 전사 내용이 아니므로 content 에 포함하지 않는다. */
	public boolean isMarker() {
		return isEndMarker() || isFinMarker();
	}

	public boolean confirmed() {
		return Boolean.TRUE.equals(isFinal);
	}

	public int startMsOrZero() {
		return startMs == null ? 0 : startMs;
	}

	public int endMsOrZero() {
		return endMs == null ? 0 : endMs;
	}
}
