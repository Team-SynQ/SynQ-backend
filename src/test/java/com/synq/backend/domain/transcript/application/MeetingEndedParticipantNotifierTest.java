package com.synq.backend.domain.transcript.application;

import com.synq.backend.domain.meeting.event.MeetingEndedEvent;
import com.synq.backend.domain.transcript.ws.SttServerMessage;
import com.synq.backend.domain.transcript.ws.SttSessionRegistry;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MeetingEndedParticipantNotifierTest {

	private final SttSessionRegistry registry = mock(SttSessionRegistry.class);
	private final MeetingEndedParticipantNotifier notifier = new MeetingEndedParticipantNotifier(registry);

	@Test
	void 회의_종료_이벤트를_받으면_구독자에게_브로드캐스트하고_연결을_정리한다() {
		notifier.handle(new MeetingEndedEvent(5L));

		verify(registry).broadcastToSubscribers(5L, SttServerMessage.meetingEnded());
		verify(registry).closeSubscribers(5L);
	}
}
