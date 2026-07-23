package com.synq.backend.domain.ai.context.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;

import com.synq.backend.domain.transcript.event.TranscriptFinalizedEvent;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class LiveContextTranscriptListenerTest {

	@Test
	void AI_갱신_실패는_전사_이벤트_발행자에게_전파하지_않는다() {
		LiveContextService liveContextService = Mockito.mock(LiveContextService.class);
		doThrow(new IllegalStateException("OpenAI timeout"))
				.when(liveContextService)
				.refresh(Mockito.any());
		LiveContextTranscriptListener listener = new LiveContextTranscriptListener(liveContextService);

		assertThatCode(() -> listener.handle(new TranscriptFinalizedEvent(
				1L, 1L, 0, 0, 1000, "확정 전사", null)))
				.doesNotThrowAnyException();
	}
}
