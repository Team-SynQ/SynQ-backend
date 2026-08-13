package com.synq.backend.domain.ai.summary.infrastructure;

import com.synq.backend.domain.ai.summary.application.MeetingSummaryService;
import com.synq.backend.domain.meeting.port.MeetingSummaryJobCreator;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** meeting 도메인의 요약 Job 접수 포트를 ai.summary 도메인이 구현한다. */
@Component
public class MeetingSummaryJobCreatorAdapter implements MeetingSummaryJobCreator {

	private final MeetingSummaryService meetingSummaryService;

	public MeetingSummaryJobCreatorAdapter(MeetingSummaryService meetingSummaryService) {
		this.meetingSummaryService = meetingSummaryService;
	}

	@Override
	public UUID createQueuedJob(Long meetingId) {
		return meetingSummaryService.queueAfterMeetingEnd(meetingId).id();
	}
}
