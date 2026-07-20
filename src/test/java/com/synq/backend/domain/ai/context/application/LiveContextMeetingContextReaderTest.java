package com.synq.backend.domain.ai.context.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.synq.backend.domain.ai.context.domain.LiveContext;
import com.synq.backend.domain.ai.context.domain.LiveContextResult;
import com.synq.backend.domain.transcript.event.TranscriptFinalizedEvent;
import com.synq.backend.domain.ai.context.repository.LiveContextRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class LiveContextMeetingContextReaderTest {

	@Test
	void 저장된_누적_요약을_회의_후_요약_도메인에_제공한다() {
		LiveContextRepository repository = Mockito.mock(LiveContextRepository.class);
		LiveContext context = LiveContext.create(
				1L,
				new LiveContextResult("회의 중 논의된 내용", "AI", List.of(), List.of(), List.of()),
				new TranscriptFinalizedEvent(1L, 1L, 0, 0, 1000, "확정 전사", null)
		);
		given(repository.findByMeetingId(1L)).willReturn(Optional.of(context));

		Optional<String> rollingSummary = new LiveContextMeetingContextReader(repository).findRollingSummary(1L);

		assertThat(rollingSummary).contains("회의 중 논의된 내용");
	}
}
