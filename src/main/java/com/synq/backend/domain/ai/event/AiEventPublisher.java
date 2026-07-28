package com.synq.backend.domain.ai.event;

import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * AI 도메인 이벤트를 SSE 전송 형식으로 변환하는 단일 진입점이다.
 */
@Component
public class AiEventPublisher {

	private final AiEventSseEmitterRegistry emitterRegistry;

	public AiEventPublisher(AiEventSseEmitterRegistry emitterRegistry) {
		this.emitterRegistry = emitterRegistry;
	}

	public void publish(Long meetingId, AiEventType type, Object data) {
		emitterRegistry.send(meetingId, AiEventPayload.of(type, meetingId, data));
	}

	public void publishHeartbeat(Long meetingId) {
		publish(meetingId, AiEventType.HEARTBEAT, Map.of("status", "alive"));
	}
}
