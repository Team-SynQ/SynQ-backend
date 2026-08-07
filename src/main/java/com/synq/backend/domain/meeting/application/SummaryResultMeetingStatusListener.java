package com.synq.backend.domain.meeting.application;

import com.synq.backend.domain.ai.event.SummaryCompletedEvent;
import com.synq.backend.domain.ai.event.SummaryFailedEvent;
import com.synq.backend.domain.meeting.service.MeetingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * ai.summary 도메인과의 결합을 요약 결과 이벤트 두 개로 제한한다.
 * 요약 완료/실패에 따라 회의 상태(SUMMARIZED / SUMMARY_FAILED)를 확정한다.
 * 상태 반영 실패는 요약 Job 상태에 영향을 주지 않도록 예외를 로깅만 한다.
 * 같은 전용 Executor에서 다시 비동기 처리하지 않아, 대기열 거절 이벤트도 즉시 반영한다.
 */
@Component
public class SummaryResultMeetingStatusListener {

	private static final Logger log = LoggerFactory.getLogger(SummaryResultMeetingStatusListener.class);

	private final MeetingService meetingService;

	public SummaryResultMeetingStatusListener(MeetingService meetingService) {
		this.meetingService = meetingService;
	}

	@EventListener
	public void onCompleted(SummaryCompletedEvent event) {
		try {
			meetingService.markSummarized(event.meetingId());
		} catch (RuntimeException e) {
			log.error("요약 완료 후 회의 상태(SUMMARIZED) 반영에 실패했습니다. meetingId={}", event.meetingId(), e);
		}
	}

	@EventListener
	public void onFailed(SummaryFailedEvent event) {
		try {
			meetingService.markSummaryFailed(event.meetingId());
		} catch (RuntimeException e) {
			log.error("요약 실패 후 회의 상태(SUMMARY_FAILED) 반영에 실패했습니다. meetingId={}", event.meetingId(), e);
		}
	}
}
