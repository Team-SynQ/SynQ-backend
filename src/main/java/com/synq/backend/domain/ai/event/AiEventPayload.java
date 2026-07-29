package com.synq.backend.domain.ai.event;

import java.time.Instant;

/**
 * 모든 SSE 이벤트가 공통으로 가지는 식별 정보와 발생 시각이다.
 */
public record AiEventPayload(
		AiEventType type,
		Long meetingId,
		Instant occurredAt,
		Object data
) {

	public static AiEventPayload of(AiEventType type, Long meetingId, Object data) {
		return new AiEventPayload(type, meetingId, Instant.now(), data);
	}
}
