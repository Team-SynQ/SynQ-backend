package com.synq.backend.domain.transcript.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.domain.transcript.config.SonioxProperties;
import com.synq.backend.domain.transcript.service.TranscriptSegmentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.time.LocalDateTime;

/**
 * 호스트 마이크 오디오 수신 핸들러. 인증/인가는 SttHandshakeInterceptor 가 이미 끝냈다.
 * 여기서는 세션을 만들고 오디오를 Soniox 로 릴레이하기만 한다.
 */
@Component
@RequiredArgsConstructor
public class HostAudioWebSocketHandler extends BinaryWebSocketHandler {

	private static final Logger log = LoggerFactory.getLogger(HostAudioWebSocketHandler.class);
	private static final String SESSION_ATTRIBUTE = "sttSession";

	private final SttSessionRegistry registry;
	private final SonioxClientFactory sonioxClientFactory;
	private final TranscriptSegmentService transcriptSegmentService;
	private final MeetingRepository meetingRepository;
	private final SonioxProperties properties;
	private final ObjectMapper objectMapper;

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		Long meetingId = (Long) session.getAttributes().get(SttHandshakeInterceptor.ATTRIBUTE_MEETING_ID);

		if (!properties.hasApiKey()) {
			log.error("SONIOX_API_KEY 가 설정되지 않아 전사를 시작할 수 없습니다. meetingId={}", meetingId);
			closeQuietly(session, CloseStatus.SERVER_ERROR.withReason("STT_UNAVAILABLE"));
			return;
		}

		LocalDateTime meetingStartedAt = meetingRepository.findById(meetingId)
				.map(Meeting::getStartedAt)
				.orElse(null);
		if (meetingStartedAt == null) {
			// 정상 플로우에서는 Meeting.of() 가 항상 startedAt 을 채우므로 있을 수 없는 상태다.
			// 여기서 0 오프셋으로 넘어가면 재연결 세그먼트가 이전 구간과 겹칠 수 있어 fail-closed 한다.
			log.error("회의에 시작 시각이 없어 전사 세션을 시작할 수 없습니다. meetingId={}", meetingId);
			closeQuietly(session, CloseStatus.SERVER_ERROR.withReason("MEETING_STARTED_AT_MISSING"));
			return;
		}

		// 이전 세션(재연결 전 연결)을 먼저 동기적으로 flush+정리한 뒤에 sequence 를 조회해야
		// (meeting_id, sequence_index) 유니크 인덱스 충돌 없이 순번을 이어받을 수 있다.
		registry.closeExisting(meetingId);

		SttSession sttSession = new SttSession(
				meetingId,
				session,
				MeetingTimeline.from(meetingStartedAt, LocalDateTime.now()),
				transcriptSegmentService.nextSequenceIndex(meetingId),
				transcriptSegmentService,
				properties,
				objectMapper,
				sonioxClientFactory
		);

		session.getAttributes().put(SESSION_ATTRIBUTE, sttSession);
		registry.register(sttSession);
		sttSession.start();
		log.info("호스트 오디오 전사 세션을 시작했습니다. meetingId={} wsSessionId={}", meetingId, session.getId());
	}

	@Override
	protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
		SttSession sttSession = sttSession(session);
		if (sttSession == null) {
			return;
		}
		byte[] payload = new byte[message.getPayload().remaining()];
		message.getPayload().get(payload);
		sttSession.relayAudio(payload);
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		SttSession sttSession = sttSession(session);
		if (sttSession == null) {
			return;
		}
		log.info("호스트 오디오 전사 세션이 종료됐습니다. meetingId={} status={}", sttSession.meetingId(), status);
		try {
			// 회의 종료(/end)가 아닌 단순 연결 끊김이다. 남은 토큰은 살리되 오래 기다리지 않는다.
			sttSession.close(true);
		} finally {
			// close() 가 예외를 던지더라도 레지스트리에 좀비 세션이 남지 않도록 반드시 제거한다.
			registry.remove(sttSession);
		}
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) {
		SttSession sttSession = sttSession(session);
		Long meetingId = sttSession == null ? null : sttSession.meetingId();
		log.warn("호스트 오디오 WebSocket 전송 오류가 발생했습니다. meetingId={}", meetingId, exception);
		closeQuietly(session, CloseStatus.SERVER_ERROR);
	}

	private SttSession sttSession(WebSocketSession session) {
		return (SttSession) session.getAttributes().get(SESSION_ATTRIBUTE);
	}

	private void closeQuietly(WebSocketSession session, CloseStatus status) {
		try {
			session.close(status);
		} catch (Exception e) {
			log.debug("WebSocket 세션 종료 중 오류: {}", e.getMessage());
		}
	}
}
