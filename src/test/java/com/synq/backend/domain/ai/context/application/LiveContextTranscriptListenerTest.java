package com.synq.backend.domain.ai.context.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.synq.backend.domain.ai.context.domain.LiveContext;
import com.synq.backend.domain.ai.context.domain.LiveContextResult;
import com.synq.backend.domain.ai.event.LiveContextUpdatedEvent;
import com.synq.backend.domain.meeting.event.MeetingEndedEvent;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.entity.MeetingStatus;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.domain.transcript.event.TranscriptFinalizedEvent;
import java.util.List;
import java.util.Optional;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.SyncTaskExecutor;

class LiveContextTranscriptListenerTest {

	@Test
	void AI_갱신_실패는_전사_이벤트_발행자에게_전파하지_않는다() {
		LiveContextService liveContextService = Mockito.mock(LiveContextService.class);
		ApplicationEventPublisher eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
		doThrow(new IllegalStateException("OpenAI timeout"))
				.when(liveContextService)
				.refreshPending(Mockito.any());
		LiveContextTranscriptListener listener = listener(liveContextService, eventPublisher);

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
		Mockito.when(liveContextService.refreshPending(1L)).thenReturn(Optional.of(
				new LiveContextRefreshResult(context, com.synq.backend.domain.ai.context.domain.AutoHintDecision.none())));
		LiveContextTranscriptListener listener = listener(liveContextService, eventPublisher);

		listener.handle(transcript);

		verify(eventPublisher).publishEvent((Object) Mockito.argThat(event -> event instanceof LiveContextUpdatedEvent updated
				&& updated.meetingId().equals(1L)
				&& updated.lastSegmentId().equals(2L)
				&& updated.lastSequenceIndex().equals(3)));
	}

	@Test
	void 확정_전사가_두개_쌓이면_한번만_배치_갱신한다() {
		LiveContextService liveContextService = Mockito.mock(LiveContextService.class);
		ApplicationEventPublisher eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
		Mockito.when(liveContextService.refreshPending(1L)).thenReturn(Optional.empty());
		LiveContextTranscriptListener listener = new LiveContextTranscriptListener(
				liveContextService, eventPublisher, properties(2), executor(), Mockito.mock(MeetingRepository.class));

		listener.handle(new TranscriptFinalizedEvent(1L, 1L, 0, 0, 1000, "첫 발화", null));
		verifyNoInteractions(liveContextService);

		listener.handle(new TranscriptFinalizedEvent(1L, 2L, 1, 1000, 2000, "둘째 발화", null));

		verify(liveContextService).refreshPending(1L);
	}

	@Test
	void 최대_대기시간이_지나면_전사_한개도_갱신한다() throws InterruptedException {
		LiveContextService liveContextService = Mockito.mock(LiveContextService.class);
		ApplicationEventPublisher eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
		when(liveContextService.refreshPending(1L)).thenReturn(Optional.empty());
		LiveContextTranscriptListener listener = new LiveContextTranscriptListener(
				liveContextService,
				eventPublisher,
				new LiveContextBatchProperties(2, 2, Duration.ofMillis(1), Duration.ofSeconds(30)),
				executor(),
				Mockito.mock(MeetingRepository.class));

		listener.handle(new TranscriptFinalizedEvent(1L, 1L, 0, 0, 1000, "첫 발화", null));
		verifyNoInteractions(liveContextService);

		Thread.sleep(5);
		listener.flushTimedOutBatches();

		verify(liveContextService).refreshPending(1L);
	}

	@Test
	void 회의가_종료되면_세그먼트_수와_무관하게_남은_전사를_갱신한다() {
		LiveContextService liveContextService = Mockito.mock(LiveContextService.class);
		ApplicationEventPublisher eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
		Mockito.when(liveContextService.refreshPending(1L)).thenReturn(Optional.empty());
		LiveContextTranscriptListener listener = new LiveContextTranscriptListener(
				liveContextService, eventPublisher, properties(2), executor(), Mockito.mock(MeetingRepository.class));

		listener.handle(new TranscriptFinalizedEvent(1L, 1L, 0, 0, 1000, "첫 발화", null));
		listener.flushOnMeetingEnd(new MeetingEndedEvent(1L));

		verify(liveContextService).refreshPending(1L);
	}

	@Test
	void 복구_주기에는_진행_중인_회의의_미반영_전사를_확인한다() {
		LiveContextService liveContextService = Mockito.mock(LiveContextService.class);
		ApplicationEventPublisher eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
		MeetingRepository meetingRepository = Mockito.mock(MeetingRepository.class);
		Meeting meeting = Mockito.mock(Meeting.class);
		when(meeting.getId()).thenReturn(1L);
		when(meetingRepository.findByStatus(MeetingStatus.IN_PROGRESS)).thenReturn(List.of(meeting));
		when(liveContextService.refreshPending(1L)).thenReturn(Optional.empty());
		LiveContextTranscriptListener listener = new LiveContextTranscriptListener(
				liveContextService, eventPublisher, properties(2), executor(), meetingRepository);

		listener.recoverPendingContexts();

		verify(liveContextService).refreshPending(1L);
	}

	private LiveContextTranscriptListener listener(
			LiveContextService liveContextService,
			ApplicationEventPublisher eventPublisher
	) {
		return new LiveContextTranscriptListener(
				liveContextService,
				eventPublisher,
				properties(1),
				executor(),
				Mockito.mock(MeetingRepository.class));
	}

	private LiveContextBatchProperties properties(int segmentCount) {
		return new LiveContextBatchProperties(segmentCount, segmentCount, Duration.ofSeconds(10), Duration.ofSeconds(30));
	}

	private MeetingTaskExecutor executor() {
		return new MeetingTaskExecutor(new SyncTaskExecutor());
	}
}
