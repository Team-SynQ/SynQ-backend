package com.synq.backend.domain.ai.event;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 중간 프록시의 유휴 연결 종료를 막기 위해 활성 회의 연결에 하트비트를 보낸다.
 */
@Component
public class AiEventHeartbeatScheduler {

	private final AiEventSseEmitterRegistry emitterRegistry;
	private final AiEventPublisher eventPublisher;

	public AiEventHeartbeatScheduler(
			AiEventSseEmitterRegistry emitterRegistry,
			AiEventPublisher eventPublisher
	) {
		this.emitterRegistry = emitterRegistry;
		this.eventPublisher = eventPublisher;
	}

	@Scheduled(fixedDelayString = "${ai.event.sse.heartbeat-interval}")
	public void sendHeartbeat() {
		for (Long meetingId : emitterRegistry.activeMeetingIds()) {
			eventPublisher.publishHeartbeat(meetingId);
		}
	}
}
