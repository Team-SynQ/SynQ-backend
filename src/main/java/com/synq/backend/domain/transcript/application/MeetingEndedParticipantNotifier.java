package com.synq.backend.domain.transcript.application;

import com.synq.backend.domain.meeting.event.MeetingEndedEvent;
import com.synq.backend.domain.transcript.ws.SttServerMessage;
import com.synq.backend.domain.transcript.ws.SttSessionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * meeting 도메인과의 결합을 MeetingEndedEvent 하나로 제한한다.
 * 정상 종료(/end)든 진행자 연결 끊김에 의한 강제 종료든 이 이벤트 하나로 들어오므로,
 * 참여자에게 회의 종료를 알리는 경로도 하나로 통일된다.
 */
@Component
@RequiredArgsConstructor
public class MeetingEndedParticipantNotifier {

	private final SttSessionRegistry registry;

	@Async("liveContextExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void handle(MeetingEndedEvent event) {
		registry.broadcastToSubscribers(event.meetingId(), SttServerMessage.meetingEnded());
		// 더 이상 내려보낼 전사가 없으므로 서버가 먼저 연결을 정리한다.
		registry.closeSubscribers(event.meetingId());
	}
}
