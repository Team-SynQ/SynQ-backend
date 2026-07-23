package com.synq.backend.domain.meeting.application;

import com.synq.backend.domain.ai.event.SummaryCompletedEvent;
import com.synq.backend.domain.ai.event.SummaryFailedEvent;
import com.synq.backend.domain.meeting.service.MeetingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * ai.summary 도메인과의 결합을 요약 결과 이벤트 두 개로 제한한다.
 * 요약 완료/실패에 따라 회의 상태(SUMMARIZED / SUMMARY_FAILED)를 확정한다.
 * 상태 반영 실패가 요약 작업(Processor) 스레드로 전파되지 않도록 별도 스레드에서 처리하고 예외는 로깅만 한다.
 */
@Component
public class SummaryResultMeetingStatusListener {

	private static final Logger log = LoggerFactory.getLogger(SummaryResultMeetingStatusListener.class);

	private final MeetingService meetingService;

	public SummaryResultMeetingStatusListener(MeetingService meetingService) {
		this.meetingService = meetingService;
	}

	@Async("summaryExecutor")
	@EventListener
	public void onCompleted(SummaryCompletedEvent event) {
		try {
			meetingService.markSummarized(event.meetingId());
		} catch (RuntimeException e) {
			log.error("요약 완료 후 회의 상태(SUMMARIZED) 반영에 실패했습니다. meetingId={}", event.meetingId(), e);
		}
	}

	@Async("summaryExecutor")
	@EventListener
	public void onFailed(SummaryFailedEvent event) {
		try {
			meetingService.markSummaryFailed(event.meetingId());
		} catch (RuntimeException e) {
			log.error("요약 실패 후 회의 상태(SUMMARY_FAILED) 반영에 실패했습니다. meetingId={}", event.meetingId(), e);
		}
	}
}
