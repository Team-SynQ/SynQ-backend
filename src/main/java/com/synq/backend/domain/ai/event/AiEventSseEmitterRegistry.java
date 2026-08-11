package com.synq.backend.domain.ai.event;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 회의별 SSE 연결을 보관하고, 연결별 순서를 보장하는 writer queue로 이벤트를 전달한다.
 */
@Component
public class AiEventSseEmitterRegistry {

	private static final Logger log = LoggerFactory.getLogger(AiEventSseEmitterRegistry.class);

	private final Map<Long, Map<UUID, SseConnection>> connectionsByMeeting = new ConcurrentHashMap<>();
	private final Executor writerExecutor;
	private final AiEventSseProperties properties;

	public AiEventSseEmitterRegistry(
			@Qualifier("sseExecutor") Executor writerExecutor,
			AiEventSseProperties properties
	) {
		this.writerExecutor = writerExecutor;
		this.properties = properties;
	}

	public SseEmitter register(Long meetingId, Long userId, long timeoutMillis) {
		SseEmitter emitter = new SseEmitter(timeoutMillis);
		UUID connectionId = UUID.randomUUID();
		SseConnection connection = new SseConnection(meetingId, userId, connectionId, emitter);

		connectionsByMeeting
				.computeIfAbsent(meetingId, ignored -> new ConcurrentHashMap<>())
				.put(connectionId, connection);

		emitter.onCompletion(() -> remove(meetingId, connectionId));
		emitter.onTimeout(() -> remove(meetingId, connectionId));
		emitter.onError(error -> remove(meetingId, connectionId));
		return emitter;
	}

	public void send(Long meetingId, AiEventPayload payload) {
		Map<UUID, SseConnection> connections = connectionsByMeeting.get(meetingId);
		if (connections == null) {
			return;
		}

		connections.forEach((connectionId, connection) -> enqueue(meetingId, connectionId, connection, payload));
	}

	/** 개인 AI 결과는 같은 회의를 구독 중인 다른 참여자에게 노출하지 않는다. */
	public void sendToUser(Long meetingId, Long userId, AiEventPayload payload) {
		Map<UUID, SseConnection> connections = connectionsByMeeting.get(meetingId);
		if (connections == null) {
			return;
		}

		connections.forEach((connectionId, connection) -> {
			if (connection.userId().equals(userId)) {
				enqueue(meetingId, connectionId, connection, payload);
			}
		});
	}

	public void sendTo(SseEmitter emitter, AiEventPayload payload) {
		connectionsByMeeting.forEach((meetingId, connections) ->
				connections.forEach((connectionId, connection) -> {
					if (connection.emitter() == emitter) {
						enqueue(meetingId, connectionId, connection, payload);
					}
				})
		);
	}

	public int activeConnectionCount(Long meetingId) {
		Map<UUID, SseConnection> connections = connectionsByMeeting.get(meetingId);
		return connections == null ? 0 : connections.size();
	}

	public Iterable<Long> activeMeetingIds() {
		return Set.copyOf(connectionsByMeeting.keySet());
	}

	private void enqueue(Long meetingId, UUID connectionId, SseConnection connection, AiEventPayload payload) {
		if (connection.enqueue(payload)) {
			return;
		}

		log.warn("SSE writer queue가 포화되었거나 실행할 수 없습니다. 연결을 종료합니다. meetingId={}, connectionId={}",
				meetingId, connectionId);
		closeAndRemove(meetingId, connectionId, connection, null);
	}

	private void remove(Long meetingId, UUID connectionId) {
		connectionsByMeeting.computeIfPresent(meetingId, (ignored, connections) -> {
			connections.remove(connectionId);
			return connections.isEmpty() ? null : connections;
		});
	}

	private void closeAndRemove(
			Long meetingId,
			UUID connectionId,
			SseConnection connection,
			Exception exception
	) {
		remove(meetingId, connectionId);
		if (exception == null) {
			connection.emitter().complete();
		} else {
			connection.emitter().completeWithError(exception);
		}
	}

	private final class SseConnection {

		private final Long meetingId;
		private final Long userId;
		private final UUID connectionId;
		private final SseEmitter emitter;
		private final Queue<AiEventPayload> pendingEvents = new ArrayDeque<>();
		private boolean writing;

		private SseConnection(Long meetingId, Long userId, UUID connectionId, SseEmitter emitter) {
			this.meetingId = meetingId;
			this.userId = userId;
			this.connectionId = connectionId;
			this.emitter = emitter;
		}

		private SseEmitter emitter() {
			return emitter;
		}

		private Long userId() {
			return userId;
		}

		private boolean enqueue(AiEventPayload payload) {
			synchronized (this) {
				if (pendingEvents.size() >= properties.queueCapacity()) {
					return false;
				}
				pendingEvents.offer(payload);
				if (writing) {
					return true;
				}
				writing = true;
			}

			try {
				writerExecutor.execute(this::drain);
				return true;
			} catch (RejectedExecutionException exception) {
				synchronized (this) {
					writing = false;
					pendingEvents.clear();
				}
				return false;
			}
		}

		private void drain() {
			while (true) {
				AiEventPayload payload = nextEvent();
				if (payload == null) {
					return;
				}

				try {
					emitter.send(SseEmitter.event()
							.id(UUID.randomUUID().toString())
							.name(payload.type().eventName())
							.data(payload));
				} catch (IOException | IllegalStateException exception) {
					log.debug("종료된 SSE 연결을 제거합니다. meetingId={}, connectionId={}", meetingId, connectionId);
					closeAndRemove(meetingId, connectionId, this, exception);
					return;
				}
			}
		}

		private AiEventPayload nextEvent() {
			synchronized (this) {
				AiEventPayload payload = pendingEvents.poll();
				if (payload == null) {
					writing = false;
				}
				return payload;
			}
		}
	}
}
