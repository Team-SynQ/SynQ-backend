package com.synq.backend.domain.meeting.application;

import com.synq.backend.domain.ai.event.SummaryCompletedEvent;
import com.synq.backend.domain.ai.event.SummaryFailedEvent;
import com.synq.backend.domain.meeting.service.MeetingService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * ai.summary 도메인과의 결합을 요약 결과 이벤트 두 개로 제한한다.
 * 요약 완료/실패에 따라 회의 상태(SUMMARIZED / SUMMARY_FAILED)를 확정한다.
 */
@Component
public class SummaryResultMeetingStatusListener {

	private final MeetingService meetingService;

	public SummaryResultMeetingStatusListener(MeetingService meetingService) {
		this.meetingService = meetingService;
	}

	@EventListener
	public void onCompleted(SummaryCompletedEvent event) {
		meetingService.markSummarized(event.meetingId());
	}

	@EventListener
	public void onFailed(SummaryFailedEvent event) {
		meetingService.markSummaryFailed(event.meetingId());
	}
}
