package com.synq.backend.domain.transcript.application;

import com.synq.backend.domain.meeting.event.MeetingPausedEvent;
import com.synq.backend.domain.meeting.event.MeetingResumedEvent;
import com.synq.backend.domain.transcript.ws.SttServerMessage;
import com.synq.backend.domain.transcript.ws.SttSessionRegistry;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MeetingPauseStateNotifierTest {

	private final SttSessionRegistry registry = mock(SttSessionRegistry.class);
	private final MeetingPauseStateNotifier notifier = new MeetingPauseStateNotifier(registry);

	@Test
	void 일시정지_이벤트를_받으면_구독자에게_activeSeconds와_함께_브로드캐스트한다() {
		notifier.handlePaused(new MeetingPausedEvent(5L, 163L));

		verify(registry).broadcastToSubscribers(5L, SttServerMessage.meetingPaused(163L));
	}

	@Test
	void 재개_이벤트를_받으면_구독자에게_activeSeconds와_함께_브로드캐스트한다() {
		notifier.handleResumed(new MeetingResumedEvent(5L, 163L));

		verify(registry).broadcastToSubscribers(5L, SttServerMessage.meetingResumed(163L));
	}
}
