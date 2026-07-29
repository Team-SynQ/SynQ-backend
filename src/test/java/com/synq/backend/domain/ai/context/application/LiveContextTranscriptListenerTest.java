package com.synq.backend.domain.ai.context.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.synq.backend.domain.ai.context.domain.LiveContext;
import com.synq.backend.domain.ai.context.domain.LiveContextResult;
import com.synq.backend.domain.ai.event.LiveContextUpdatedEvent;
import com.synq.backend.domain.transcript.event.TranscriptFinalizedEvent;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

class LiveContextTranscriptListenerTest {

	@Test
	void AI_갱신_실패는_전사_이벤트_발행자에게_전파하지_않는다() {
		LiveContextService liveContextService = Mockito.mock(LiveContextService.class);
		ApplicationEventPublisher eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
		doThrow(new IllegalStateException("OpenAI timeout"))
				.when(liveContextService)
				.refresh(Mockito.any());
		LiveContextTranscriptListener listener = new LiveContextTranscriptListener(liveContextService, eventPublisher);

		assertThatCode(() -> listener.handle(new TranscriptFinalizedEvent(
				1L, 1L, 0, 0, 1000, "확정 전사", null)))
				.doesNotThrowAnyException();
	}

	@Test
	void Live_Context가_갱신되면_SSE_전달용_이벤트를_발행한다() {
		LiveContextService liveContextService = Mockito.mock(LiveContextService.class);
		ApplicationEventPublisher eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
		TranscriptFinalizedEvent transcript = new TranscriptFinalizedEvent(
				1L, 2L, 3, 0, 1000, "확정 전사", null);
		LiveContext context = LiveContext.create(
				1L,
				new LiveContextResult("누적 요약", "현재 주제", List.of("결정"), List.of(), List.of()),
				transcript
		);
		Mockito.when(liveContextService.refresh(transcript)).thenReturn(Optional.of(context));
		LiveContextTranscriptListener listener = new LiveContextTranscriptListener(liveContextService, eventPublisher);

		listener.handle(transcript);

		verify(eventPublisher).publishEvent((Object) Mockito.argThat(event -> event instanceof LiveContextUpdatedEvent updated
				&& updated.meetingId().equals(1L)
				&& updated.lastSegmentId().equals(2L)
				&& updated.lastSequenceIndex().equals(3)));
	}
}
