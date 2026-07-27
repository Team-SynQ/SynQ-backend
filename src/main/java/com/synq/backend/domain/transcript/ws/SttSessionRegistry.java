package com.synq.backend.domain.transcript.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** meetingId 기준 전사 세션 레지스트리. 회의 하나에 호스트 스트림 하나만 존재한다. */
@Component
public class SttSessionRegistry {

	private static final Logger log = LoggerFactory.getLogger(SttSessionRegistry.class);

	private final Map<Long, SttSession> sessions = new ConcurrentHashMap<>();

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
}
