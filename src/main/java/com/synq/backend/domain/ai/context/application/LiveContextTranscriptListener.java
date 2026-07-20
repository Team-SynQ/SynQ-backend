package com.synq.backend.domain.ai.context.application;

import com.synq.backend.domain.transcript.event.TranscriptFinalizedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 전사 도메인과의 결합을 이벤트 계약 하나로 제한한다.
 * 추후 transcript 저장 서비스가 확정 세그먼트 저장 뒤 이 이벤트를 발행하면 된다.
 */
@Component
public class LiveContextTranscriptListener {

	private static final Logger log = LoggerFactory.getLogger(LiveContextTranscriptListener.class);

	private final LiveContextService liveContextService;

	public LiveContextTranscriptListener(LiveContextService liveContextService) {
		this.liveContextService = liveContextService;
	}

	@Async("liveContextExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void handle(TranscriptFinalizedEvent event) {
		try {
			liveContextService.refresh(event);
		} catch (RuntimeException e) {
			// AI 갱신 실패가 이미 저장된 STT 전사를 실패 처리하지 않도록 예외를 전파하지 않는다.
			log.error("회의 Live Context 갱신에 실패했습니다. meetingId={}, segmentId={}",
				event.meetingId(), event.segmentId(), e);
		}
	}
}
