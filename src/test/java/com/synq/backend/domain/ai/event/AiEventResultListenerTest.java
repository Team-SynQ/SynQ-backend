package com.synq.backend.domain.ai.event;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.synq.backend.domain.ai.context.domain.LiveContextSnapshot;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AiEventResultListenerTest {

	private final AiEventPublisher eventPublisher = Mockito.mock(AiEventPublisher.class);
	private final AiEventResultListener listener = new AiEventResultListener(eventPublisher);

	@Test
	void 요약_완료_이벤트를_SSE_이벤트로_변환한다() {
		UUID jobId = UUID.randomUUID();

		listener.onSummaryCompleted(new SummaryCompletedEvent(1L, jobId));

		verify(eventPublisher).publish(eq(1L), eq(AiEventType.SUMMARY_COMPLETED), Mockito.argThat(data ->
				data instanceof java.util.Map<?, ?> map && map.get("jobId").equals(jobId)));
	}

	@Test
	void Live_Context_갱신_이벤트를_SSE_이벤트로_변환한다() {
		listener.onLiveContextUpdated(new LiveContextUpdatedEvent(
				1L,
				new LiveContextSnapshot("요약", "주제", List.of("결정"), List.of("할 일"), List.of("질문")),
				2L,
				3
		));

		verify(eventPublisher).publish(eq(1L), eq(AiEventType.LIVE_CONTEXT_UPDATED), Mockito.any());
	}
}
