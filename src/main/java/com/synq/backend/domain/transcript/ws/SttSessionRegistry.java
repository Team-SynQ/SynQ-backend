package com.synq.backend.domain.transcript.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * meetingId 기준 전사 세션 레지스트리. 회의 하나에 호스트 오디오 스트림은 하나만 존재하지만,
 * 수신 전용 참여자 구독은 여러 명일 수 있어 별도 맵으로 관리한다. 두 맵은 독립적인 생명주기를 가진다
 * (호스트 재연결/종료가 참여자 구독 목록을 건드리지 않고, 그 반대도 마찬가지).
 */
@Component
public class SttSessionRegistry {

	private static final Logger log = LoggerFactory.getLogger(SttSessionRegistry.class);

	private final Map<Long, SttSession> sessions = new ConcurrentHashMap<>();
	private final Map<Long, Set<WebSocketSession>> subscribers = new ConcurrentHashMap<>();
	private final ObjectMapper objectMapper;

	public SttSessionRegistry(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/**
	 * 같은 회의에 이미 세션이 있으면 끊긴 이전 연결로 보고 동기적으로 정리한다 (flush 완료까지 대기).
	 * 호스트는 한 명이므로 중복 등록은 재연결이거나 중복 탭이다.
	 *
	 * <p>새 세션을 만들기 전에 반드시 호출해야 한다. 이전 세션이 아직 살아있는 상태에서 새 세션의
	 * sequence 시작값을 DB에서 조회하면, 이전 세션의 미확정 버퍼가 나중에 flush 되면서 같은
	 * sequence_index 로 저장을 시도해 유니크 인덱스 충돌이 날 수 있다.
	 */
	public void closeExisting(Long meetingId) {
		SttSession previous = sessions.remove(meetingId);
		if (previous != null) {
			log.info("기존 전사 세션을 정리하고 새 연결로 교체합니다. meetingId={}", meetingId);
			previous.close(false);
		}
	}

	public void register(SttSession session) {
		sessions.put(session.meetingId(), session);
	}

	/** 등록된 세션과 동일한 브라우저 연결일 때만 제거한다. 재연결로 교체된 새 세션을 지우지 않기 위함이다. */
	public void remove(SttSession session) {
		sessions.remove(session.meetingId(), session);
	}

	public Optional<SttSession> find(Long meetingId) {
		return Optional.ofNullable(sessions.get(meetingId));
	}

	public Collection<SttSession> activeSessions() {
		return List.copyOf(sessions.values());
	}

	public void registerSubscriber(Long meetingId, WebSocketSession session) {
		subscribers.computeIfAbsent(meetingId, id -> ConcurrentHashMap.newKeySet()).add(session);
	}

	/** 구독 세션이 끊겼을 때만 호출한다. 목록이 비어도 meetingId 엔트리 자체는 남겨둬도 무방하다(가벼운 빈 Set). */
	public void removeSubscriber(Long meetingId, WebSocketSession session) {
		Set<WebSocketSession> meetingSubscribers = subscribers.get(meetingId);
		if (meetingSubscribers != null) {
			meetingSubscribers.remove(session);
		}
	}

	/** 등록된 구독자 전원에게 같은 메시지를 보낸다. 개별 전송 실패는 로그만 남기고 나머지 구독자에게는 계속 보낸다. */
	public void broadcastToSubscribers(Long meetingId, SttServerMessage message) {
		Set<WebSocketSession> meetingSubscribers = subscribers.get(meetingId);
		if (meetingSubscribers == null || meetingSubscribers.isEmpty()) {
			return;
		}
		String payload;
		try {
			payload = objectMapper.writeValueAsString(message);
		} catch (IOException e) {
			log.warn("구독자 브로드캐스트 메시지 직렬화에 실패했습니다. meetingId={}", meetingId, e);
			return;
		}
		for (WebSocketSession session : meetingSubscribers) {
			sendQuietly(meetingId, session, payload);
		}
	}

	private void sendQuietly(Long meetingId, WebSocketSession session, String payload) {
		if (!session.isOpen()) {
			return;
		}
		try {
			// WebSocketSession 은 동시 전송에 안전하지 않다. 다른 스레드가 같은 세션에 쓸 수 있으니 직렬화한다.
			synchronized (session) {
				session.sendMessage(new TextMessage(payload));
			}
		} catch (IOException | RuntimeException e) {
			log.warn("구독자에게 전사 메시지를 보내지 못했습니다. meetingId={} wsSessionId={}",
					meetingId, session.getId(), e);
		}
	}
}
