package com.synq.backend.domain.ai.event;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 회의별 SSE 연결을 보관하고, 같은 회의의 구독자에게 이벤트를 전달한다.
 */
@Component
public class AiEventSseEmitterRegistry {

	private static final Logger log = LoggerFactory.getLogger(AiEventSseEmitterRegistry.class);

	private final Map<Long, Map<UUID, SseEmitter>> emittersByMeeting = new ConcurrentHashMap<>();

	public SseEmitter register(Long meetingId, long timeoutMillis) {
		SseEmitter emitter = new SseEmitter(timeoutMillis);
		UUID connectionId = UUID.randomUUID();

		emittersByMeeting
				.computeIfAbsent(meetingId, ignored -> new ConcurrentHashMap<>())
				.put(connectionId, emitter);

		emitter.onCompletion(() -> remove(meetingId, connectionId));
		emitter.onTimeout(() -> remove(meetingId, connectionId));
		emitter.onError(error -> remove(meetingId, connectionId));
		return emitter;
	}

	public void send(Long meetingId, AiEventPayload payload) {
		Map<UUID, SseEmitter> emitters = emittersByMeeting.get(meetingId);
		if (emitters == null) {
			return;
		}

		emitters.forEach((connectionId, emitter) -> {
			try {
				emitter.send(SseEmitter.event()
						.id(UUID.randomUUID().toString())
						.name(payload.type().eventName())
						.data(payload));
			} catch (IOException | IllegalStateException exception) {
				log.debug("종료된 SSE 연결을 제거합니다. meetingId={}, connectionId={}", meetingId, connectionId);
				remove(meetingId, connectionId);
			}
		});
	}

	public void sendTo(SseEmitter emitter, AiEventPayload payload) {
		try {
			emitter.send(SseEmitter.event()
					.id(UUID.randomUUID().toString())
					.name(payload.type().eventName())
					.data(payload));
		} catch (IOException | IllegalStateException exception) {
			emitter.completeWithError(exception);
			removeEmitter(emitter);
		}
	}

	public int activeConnectionCount(Long meetingId) {
		Map<UUID, SseEmitter> emitters = emittersByMeeting.get(meetingId);
		return emitters == null ? 0 : emitters.size();
	}

	public Iterable<Long> activeMeetingIds() {
		return Set.copyOf(emittersByMeeting.keySet());
	}

	private void remove(Long meetingId, UUID connectionId) {
		emittersByMeeting.computeIfPresent(meetingId, (ignored, emitters) -> {
			emitters.remove(connectionId);
			return emitters.isEmpty() ? null : emitters;
		});
	}

	private void removeEmitter(SseEmitter emitter) {
		emittersByMeeting.forEach((meetingId, emitters) -> emitters.forEach((connectionId, registered) -> {
			if (registered == emitter) {
				remove(meetingId, connectionId);
			}
		}));
	}
}
