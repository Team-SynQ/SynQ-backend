package com.synq.backend.domain.meeting.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.synq.backend.domain.ai.event.SummaryCompletedEvent;
import com.synq.backend.domain.ai.event.SummaryFailedEvent;
import com.synq.backend.domain.meeting.service.MeetingService;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SummaryResultMeetingStatusListenerTest {

	@Test
	void 요약_완료_이벤트를_받으면_회의를_완료_상태로_전이한다() {
		MeetingService meetingService = mock(MeetingService.class);
		var listener = new SummaryResultMeetingStatusListener(meetingService);

		listener.onCompleted(new SummaryCompletedEvent(1L, UUID.randomUUID()));

		verify(meetingService).markSummarized(1L);
	}

	@Test
	void 요약_실패_이벤트를_받으면_회의를_실패_상태로_전이한다() {
		MeetingService meetingService = mock(MeetingService.class);
		var listener = new SummaryResultMeetingStatusListener(meetingService);

		listener.onFailed(new SummaryFailedEvent(1L, UUID.randomUUID(), "요약 작업 대기열이 가득 찼습니다."));

		verify(meetingService).markSummaryFailed(1L);
	}
}
