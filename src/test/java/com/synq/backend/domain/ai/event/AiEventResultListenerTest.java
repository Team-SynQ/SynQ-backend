package com.synq.backend.domain.ai.event;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.synq.backend.domain.ai.context.domain.LiveContextSnapshot;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

		ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
		verify(eventPublisher).publish(eq(1L), eq(AiEventType.LIVE_CONTEXT_UPDATED), payloadCaptor.capture());

		LiveContextUpdatedPayload payload = (LiveContextUpdatedPayload) payloadCaptor.getValue();
		org.assertj.core.api.Assertions.assertThat(payload).isEqualTo(new LiveContextUpdatedPayload(
				1L, "요약", "주제", List.of("결정"), List.of("할 일"), List.of("질문"), 2L, 3
		));
	}

	@Test
	void 요약_실패_이벤트를_SSE_이벤트로_변환한다() {
		UUID jobId = UUID.randomUUID();

		listener.onSummaryFailed(new SummaryFailedEvent(1L, jobId, "안전한 실패 메시지"));

		ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
		verify(eventPublisher).publish(eq(1L), eq(AiEventType.SUMMARY_FAILED), payloadCaptor.capture());
		Map<?, ?> payload = (Map<?, ?>) payloadCaptor.getValue();
		org.assertj.core.api.Assertions.assertThat(payload.get("jobId")).isEqualTo(jobId);
		org.assertj.core.api.Assertions.assertThat(payload.get("reason")).isEqualTo("안전한 실패 메시지");
	}
}
