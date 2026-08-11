package com.synq.backend.domain.ai.context.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.synq.backend.domain.ai.context.domain.LiveContext;
import com.synq.backend.domain.ai.context.domain.LiveContextAiClient;
import com.synq.backend.domain.ai.context.domain.LiveContextResult;
import com.synq.backend.domain.ai.context.repository.LiveContextRepository;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.domain.transcript.entity.TranscriptSegment;
import com.synq.backend.domain.transcript.event.TranscriptFinalizedEvent;
import com.synq.backend.domain.transcript.repository.TranscriptSegmentRepository;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.data.domain.Pageable;

class OneHourLiveContextFlowTest {

	private static final long MEETING_ID = 1L;
	private static final int SEGMENT_COUNT = 180;

	@Test
	void 한시간_전사_180개는_두개씩_총_90번_Live_Context_갱신을_요청한다() {
		LiveContextService liveContextService = mock(LiveContextService.class);
		LiveContextTranscriptListener listener = new LiveContextTranscriptListener(
				liveContextService,
				mock(ApplicationEventPublisher.class),
				new LiveContextBatchProperties(2, 2, Duration.ofSeconds(10), Duration.ofSeconds(30)),
				new MeetingTaskExecutor(new SyncTaskExecutor()),
				mock(MeetingRepository.class));

		for (int sequence = 0; sequence < SEGMENT_COUNT; sequence++) {
			listener.handle(event(sequence));
		}

		verify(liveContextService, org.mockito.Mockito.times(90)).refreshPending(MEETING_ID);
	}

	@Test
	void 한시간_전사의_미반영_구간은_두개씩_나누어_반영한다() {
		List<TranscriptSegment> segments = oneHourSegments();
		LiveContextRepository liveContextRepository = mock(LiveContextRepository.class);
		TranscriptSegmentRepository transcriptSegmentRepository = mock(TranscriptSegmentRepository.class);
		LiveContextAiClient aiClient = mock(LiveContextAiClient.class);
		AtomicReference<LiveContext> stored = new AtomicReference<>();

		when(liveContextRepository.findByMeetingId(MEETING_ID))
				.thenAnswer(invocation -> Optional.ofNullable(stored.get()));
		when(transcriptSegmentRepository.findByMeetingIdAndSequenceIndexGreaterThanOrderBySequenceIndexAsc(
				eq(MEETING_ID), anyInt(), any(Pageable.class)))
				.thenAnswer(invocation -> segments.stream()
						.filter(segment -> segment.getSequenceIndex() > invocation.getArgument(1, Integer.class))
						.toList());
		when(liveContextRepository.saveAndFlush(any(LiveContext.class))).thenAnswer(invocation -> {
			LiveContext context = invocation.getArgument(0, LiveContext.class);
			stored.set(context);
			return context;
		});
		when(aiClient.refresh(any(), any())).thenReturn(new LiveContextResult(
				"1시간 회의 맥락", "릴리스 준비", List.of("일정 확정"), List.of(), List.of()));

		LiveContextService service = new LiveContextService(
				liveContextRepository,
				aiClient,
				transcriptSegmentRepository,
				new LiveContextBatchProperties(2, 2, Duration.ofSeconds(10), Duration.ofSeconds(30)));

		var first = service.refreshPending(MEETING_ID).orElseThrow();
		var second = service.refreshPending(MEETING_ID).orElseThrow();

		ArgumentCaptor<TranscriptFinalizedEvent> eventCaptor = ArgumentCaptor.forClass(TranscriptFinalizedEvent.class);
		verify(aiClient, org.mockito.Mockito.times(2)).refresh(any(), eventCaptor.capture());
		assertThat(eventCaptor.getAllValues().get(0).content())
				.contains("[segmentId=1]")
				.contains("[segmentId=2]")
				.doesNotContain("[segmentId=3]");
		assertThat(eventCaptor.getAllValues().get(1).content())
				.contains("[segmentId=3]")
				.contains("[segmentId=4]")
				.doesNotContain("[segmentId=5]");
		assertThat(first.hasMorePending()).isTrue();
		assertThat(second.hasMorePending()).isTrue();
		assertThat(stored.get().getLastSequenceIndex()).isEqualTo(3);
		assertThat(stored.get().getLastSegmentId()).isEqualTo(4L);
	}

	private TranscriptFinalizedEvent event(int sequence) {
		int startMs = sequence * 20_000;
		return new TranscriptFinalizedEvent(
				MEETING_ID,
				(long) sequence + 1,
				sequence,
				startMs,
				startMs + 20_000,
				"%02d분 %02d초 발화".formatted(startMs / 60_000, (startMs / 1_000) % 60),
				null);
	}

	private List<TranscriptSegment> oneHourSegments() {
		List<TranscriptSegment> segments = new ArrayList<>();
		for (int sequence = 0; sequence < SEGMENT_COUNT; sequence++) {
			TranscriptSegment segment = mock(TranscriptSegment.class);
			int startMs = sequence * 20_000;
			when(segment.getId()).thenReturn((long) sequence + 1);
			when(segment.getSequenceIndex()).thenReturn(sequence);
			when(segment.getStartMs()).thenReturn(startMs);
			when(segment.getEndMs()).thenReturn(startMs + 20_000);
			when(segment.getContent()).thenReturn(
					"%02d분 %02d초 발화".formatted(startMs / 60_000, (startMs / 1_000) % 60));
			segments.add(segment);
		}
		return segments;
	}
}
