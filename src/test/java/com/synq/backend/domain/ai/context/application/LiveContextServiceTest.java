package com.synq.backend.domain.ai.context.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.synq.backend.domain.ai.context.domain.LiveContext;
import com.synq.backend.domain.ai.context.domain.LiveContextAiClient;
import com.synq.backend.domain.ai.context.domain.LiveContextResult;
import com.synq.backend.domain.transcript.event.TranscriptFinalizedEvent;
import com.synq.backend.domain.ai.context.repository.LiveContextRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LiveContextServiceTest {

	@Mock
	private LiveContextRepository repository;

	@Mock
	private LiveContextAiClient aiClient;

	@Test
	void 처음_들어온_확정_전사로_회의_맥락을_생성한다() {
		TranscriptFinalizedEvent event = event(1L, 10L, 0, "회의 후 AI 요약 API를 구현합시다.");
		given(repository.findByMeetingId(1L)).willReturn(Optional.empty());
		given(aiClient.refresh(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(event)))
				.willReturn(result("회의 후 AI 요약 API 구현을 논의했다."));
		given(repository.saveAndFlush(org.mockito.ArgumentMatchers.any(LiveContext.class)))
				.willAnswer(invocation -> invocation.getArgument(0));

		LiveContext context = new LiveContextService(repository, aiClient).refresh(event).orElseThrow();

		assertThat(context.getMeetingId()).isEqualTo(1L);
		assertThat(context.getRollingSummary()).isEqualTo("회의 후 AI 요약 API 구현을 논의했다.");
		assertThat(context.getLastSequenceIndex()).isZero();
		verify(aiClient).refresh(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(event));
	}

	@Test
	void 이미_처리한_순번의_전사는_무시한다() {
		TranscriptFinalizedEvent first = event(1L, 10L, 0, "첫 번째 확정 전사");
		LiveContext context = LiveContext.create(1L, result("첫 번째 맥락"), first);
		TranscriptFinalizedEvent duplicate = event(1L, 11L, 0, "재전송된 첫 번째 전사");
		given(repository.findByMeetingId(1L)).willReturn(Optional.of(context));

		Optional<LiveContext> refreshed = new LiveContextService(repository, aiClient).refresh(duplicate);

		assertThat(refreshed).isEmpty();
		verifyNoInteractions(aiClient);
	}

	private static TranscriptFinalizedEvent event(
			Long meetingId, Long segmentId, int sequenceIndex, String content
	) {
		return new TranscriptFinalizedEvent(meetingId, segmentId, sequenceIndex, 0, 1000, content, null);
	}

	private static LiveContextResult result(String summary) {
		return new LiveContextResult(summary, "AI 기능 구현", List.of("요약 API를 구현한다."), List.of(), List.of());
	}
}
