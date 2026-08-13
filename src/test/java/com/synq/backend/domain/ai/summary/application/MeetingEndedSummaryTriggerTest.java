package com.synq.backend.domain.ai.summary.application;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.synq.backend.domain.meeting.event.MeetingEndedEvent;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MeetingEndedSummaryTriggerTest {

	@Test
	void 종료_이벤트에_담긴_Job만_비동기_실행한다() {
		MeetingSummaryService service = Mockito.mock(MeetingSummaryService.class);
		MeetingEndedSummaryTrigger trigger = new MeetingEndedSummaryTrigger(service);
		UUID jobId = UUID.randomUUID();

		trigger.handle(new MeetingEndedEvent(1L, jobId));

		verify(service).startAfterMeetingEnd(jobId);
		verifyNoMoreInteractions(service);
	}
}
