package com.synq.backend.domain.transcript.application;

import com.synq.backend.domain.meeting.event.MeetingEndedEvent;
import com.synq.backend.domain.transcript.ws.SttSessionRegistry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * meeting 도메인과의 결합을 MeetingEndedEvent 하나로 제한한다.
 * 회의 종료가 커밋되면 Soniox 스트림을 정리한다.
 */
@Component
@RequiredArgsConstructor
public class MeetingEndedSttCleanupListener {

	private static final Logger log = LoggerFactory.getLogger(MeetingEndedSttCleanupListener.class);

	private final SttSessionRegistry registry;

	@Async("liveContextExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void handle(MeetingEndedEvent event) {
		registry.find(event.meetingId()).ifPresent(session -> {
			try {
				// 빈 프레임을 보내고 Soniox 가 남긴 마지막 토큰까지 받은 뒤 닫는다.
				session.close(true);
			} catch (RuntimeException e) {
				log.error("회의 종료 시 전사 세션 정리에 실패했습니다. meetingId={}", event.meetingId(), e);
			} finally {
				registry.remove(session);
			}
		});
	}
}
