package com.synq.backend.domain.ai.summary.application;

import com.synq.backend.domain.meeting.event.MeetingEndedEvent;
import com.synq.backend.global.apipayload.exception.GeneralException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * meeting 도메인과의 결합을 MeetingEndedEvent 하나로 제한한다.
 * 회의 종료가 커밋된 뒤 AI 정리 생성을 접수한다.
 */
@Component
public class MeetingEndedSummaryTrigger {

	private static final Logger log = LoggerFactory.getLogger(MeetingEndedSummaryTrigger.class);

	private final MeetingSummaryService meetingSummaryService;

	public MeetingEndedSummaryTrigger(MeetingSummaryService meetingSummaryService) {
		this.meetingSummaryService = meetingSummaryService;
	}

	@Async("summaryExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void handle(MeetingEndedEvent event) {
		try {
			meetingSummaryService.request(event.meetingId());
		} catch (GeneralException e) {
			// 이미 진행 중인 요약 작업이 있는 등(CONFLICT) 접수 실패는 회의 종료 자체를 되돌리지 않는다.
			log.warn("회의 종료 이벤트로 요약을 시작하지 못했습니다. meetingId={}", event.meetingId(), e);
		}
	}
}
