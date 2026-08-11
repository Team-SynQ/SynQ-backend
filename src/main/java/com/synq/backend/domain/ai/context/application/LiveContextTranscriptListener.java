package com.synq.backend.domain.ai.context.application;

import com.synq.backend.domain.ai.context.domain.LiveContextSnapshot;
import com.synq.backend.domain.ai.event.LiveContextUpdatedEvent;
import com.synq.backend.domain.meeting.event.MeetingEndedEvent;
import com.synq.backend.domain.meeting.entity.MeetingStatus;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.domain.transcript.event.TranscriptFinalizedEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.ApplicationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
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
	private final ApplicationEventPublisher eventPublisher;
	private final LiveContextBatchProperties properties;
	private final MeetingTaskExecutor meetingTaskExecutor;
	private final MeetingRepository meetingRepository;
	private final Map<Long, PendingBatch> pendingBatches = new ConcurrentHashMap<>();

	public LiveContextTranscriptListener(
			LiveContextService liveContextService,
			ApplicationEventPublisher eventPublisher,
			LiveContextBatchProperties properties,
			MeetingTaskExecutor meetingTaskExecutor,
			MeetingRepository meetingRepository
	) {
		this.liveContextService = liveContextService;
		this.eventPublisher = eventPublisher;
		this.properties = properties;
		this.meetingTaskExecutor = meetingTaskExecutor;
		this.meetingRepository = meetingRepository;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void handle(TranscriptFinalizedEvent event) {
		int count = pendingBatches.compute(event.meetingId(), (meetingId, pending) ->
				pending == null ? new PendingBatch(1, Instant.now()) : pending.next()).count();
		if (count >= properties.segmentCount()) {
			dispatchIfPending(event.meetingId());
		}
	}

	@Scheduled(fixedDelay = 1_000)
	public void flushTimedOutBatches() {
		Instant now = Instant.now();
		pendingBatches.forEach((meetingId, pending) -> {
			if (Duration.between(pending.firstQueuedAt(), now).compareTo(properties.maxDelay()) >= 0) {
				dispatchIfPending(meetingId);
			}
		});
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void flushOnMeetingEnd(MeetingEndedEvent event) {
		pendingBatches.remove(event.meetingId());
		submitRefresh(event.meetingId());
	}

	/** 서버 교체 뒤에도 진행 중 회의의 DB 미반영 전사를 다시 발견한다. */
	@Scheduled(fixedDelayString = "${ai.live-context.batch.recovery-delay:30s}")
	public void recoverPendingContexts() {
		meetingRepository.findByStatus(MeetingStatus.IN_PROGRESS)
				.forEach(meeting -> submitRefresh(meeting.getId()));
	}

	private void dispatchIfPending(Long meetingId) {
		if (pendingBatches.remove(meetingId) != null) {
			submitRefresh(meetingId);
		}
	}

	private void submitRefresh(Long meetingId) {
		try {
			meetingTaskExecutor.execute(meetingId, () -> refreshAndPublish(meetingId));
		} catch (RuntimeException exception) {
			log.error("회의 Live Context 작업을 제출하지 못했습니다. meetingId={}", meetingId, exception);
			pendingBatches.putIfAbsent(meetingId, new PendingBatch(1, Instant.now()));
		}
	}

	private void refreshAndPublish(Long meetingId) {
		try {
			liveContextService.refreshPending(meetingId).ifPresent(refreshResult -> {
				eventPublisher.publishEvent(new LiveContextUpdatedEvent(
						meetingId,
						LiveContextSnapshot.from(refreshResult.context()),
						refreshResult.context().getLastSegmentId(),
						refreshResult.context().getLastSequenceIndex(),
						refreshResult.autoHintDecision()
				));
				// 밀린 전사는 같은 회의의 순차 큐에서 다음 묶음으로 이어 처리한다.
				if (refreshResult.hasMorePending()) {
					submitRefresh(meetingId);
				}
			});
		} catch (RuntimeException e) {
			log.error("회의 Live Context 배치 갱신에 실패했습니다. meetingId={}", meetingId, e);
			// 마지막 반영 순번은 그대로라 다음 배치에서 DB 전사를 다시 읽는다. 일시적인 외부 AI
			// 장애로 회의가 끝날 때까지 맥락 갱신이 멈추지 않도록 재시도 대기열에 넣는다.
			pendingBatches.putIfAbsent(meetingId, new PendingBatch(1, Instant.now()));
		}
	}

	private record PendingBatch(int count, Instant firstQueuedAt) {
		private PendingBatch next() {
			return new PendingBatch(count + 1, firstQueuedAt);
		}
	}
}
