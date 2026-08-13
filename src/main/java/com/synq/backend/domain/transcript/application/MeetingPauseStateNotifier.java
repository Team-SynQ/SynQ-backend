package com.synq.backend.domain.transcript.application;

import com.synq.backend.domain.meeting.event.MeetingPausedEvent;
import com.synq.backend.domain.meeting.event.MeetingResumedEvent;
import com.synq.backend.domain.transcript.ws.SttServerMessage;
import com.synq.backend.domain.transcript.ws.SttSessionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * meeting 도메인과의 결합을 MeetingPausedEvent/MeetingResumedEvent 로 제한한다.
 * 진행자의 일시정지/재개를 참여자에게 알려, 참여자 화면의 타이머가 진행자와 어긋나지 않게 한다.
 */
@Component
@RequiredArgsConstructor
public class MeetingPauseStateNotifier {

	private final SttSessionRegistry registry;

	@Async("liveContextExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void handlePaused(MeetingPausedEvent event) {
		registry.broadcastToSubscribers(event.meetingId(), SttServerMessage.meetingPaused(event.activeSeconds()));
	}

	@Async("liveContextExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void handleResumed(MeetingResumedEvent event) {
		registry.broadcastToSubscribers(event.meetingId(), SttServerMessage.meetingResumed(event.activeSeconds()));
	}
}
