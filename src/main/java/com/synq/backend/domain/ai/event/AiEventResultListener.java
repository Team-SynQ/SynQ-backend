package com.synq.backend.domain.ai.event;

import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * AI 처리 완료 이벤트를 프론트엔드가 구독하는 SSE 이벤트로 전달한다.
 */
@Component
public class AiEventResultListener {

	private final AiEventPublisher eventPublisher;

	public AiEventResultListener(AiEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	@EventListener
	public void onLiveContextUpdated(LiveContextUpdatedEvent event) {
		eventPublisher.publish(event.meetingId(), AiEventType.LIVE_CONTEXT_UPDATED,
				new LiveContextUpdatedPayload(
						event.meetingId(),
						event.context().rollingSummary(),
						event.context().currentTopic(),
						event.context().decisions(),
						event.context().actionItems(),
						event.context().openQuestions(),
						event.lastSegmentId(),
						event.lastSequenceIndex()
				));
	}

	@EventListener
	public void onSummaryCompleted(SummaryCompletedEvent event) {
		eventPublisher.publish(event.meetingId(), AiEventType.SUMMARY_COMPLETED,
				Map.of("jobId", event.jobId()));
	}

	@EventListener
	public void onSummaryFailed(SummaryFailedEvent event) {
		eventPublisher.publish(event.meetingId(), AiEventType.SUMMARY_FAILED,
				Map.of("jobId", event.jobId(), "reason", event.reason()));
	}
}
