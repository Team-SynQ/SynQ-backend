package com.synq.backend.domain.ai.assistant.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.synq.backend.domain.ai.assistant.domain.HintResult;
import com.synq.backend.domain.ai.assistant.domain.SegmentHint;
import com.synq.backend.domain.ai.assistant.service.HintService;
import com.synq.backend.domain.ai.context.domain.AutoHintDecision;
import com.synq.backend.domain.ai.context.domain.LiveContextSnapshot;
import com.synq.backend.domain.ai.event.AutoHintCreatedEvent;
import com.synq.backend.domain.ai.event.LiveContextUpdatedEvent;
import com.synq.backend.domain.meeting.entity.MeetingParticipant;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.entity.MeetingStatus;
import com.synq.backend.domain.meeting.entity.ParticipantRole;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.SyncTaskExecutor;

class AutoHintGenerationListenerTest {

	private final MeetingParticipantRepository participantRepository = Mockito.mock(MeetingParticipantRepository.class);
	private final MeetingRepository meetingRepository = Mockito.mock(MeetingRepository.class);
	private final HintService hintService = Mockito.mock(HintService.class);
	private final ApplicationEventPublisher eventPublisher = Mockito.mock(ApplicationEventPublisher.class);

	@Test
	void 중요도가_기준_이상이면_활성_참여자별로_자동_힌트를_생성하고_개인_이벤트를_발행한다() {
		inProgressMeeting();
		when(participantRepository.findByMeetingIdAndLeftAtIsNull(1L)).thenReturn(List.of(
				MeetingParticipant.of(1L, 10L, ParticipantRole.HOST),
				MeetingParticipant.of(1L, 20L, ParticipantRole.MEMBER)
		));
		HintResult result = new HintResult("의미", "영향", "질문");
		when(hintService.generateAutomatically(any(), eq(1L), eq(3L))).thenReturn(result);
		when(hintService.saveAutomatically(eq(1L), eq(3L), any(), eq(result), eq(80), eq("일정이 확정됨"), any()))
				.thenReturn(Optional.of(SegmentHint.autoOf(1L, 3L, 10L, result, 80, "일정이 확정됨", null)));

		listener(60).generate(event(80, true));

		verify(hintService).generateAutomatically(10L, 1L, 3L);
		verify(hintService).generateAutomatically(20L, 1L, 3L);
		verify(eventPublisher, org.mockito.Mockito.times(2)).publishEvent(any(AutoHintCreatedEvent.class));
	}

	@Test
	void 최근_자동_힌트와_주제와_판단_사유가_같으면_AI_호출을_생략한다() {
		inProgressMeeting();
		when(participantRepository.findByMeetingIdAndLeftAtIsNull(1L)).thenReturn(List.of(
				MeetingParticipant.of(1L, 10L, ParticipantRole.HOST)));
		when(hintService.hasRecentAutomaticDuplicate(1L, 10L, null, "일정이 확정됨")).thenReturn(true);

		listener(60).generate(event(80, true));

		verify(hintService, never()).generateAutomatically(any(), any(), any());
	}

	@Test
	void 중요도가_기준보다_낮으면_참여자_조회나_AI_호출을_하지_않는다() {
		listener(60).generate(event(59, true));

		verify(participantRepository, never()).findByMeetingIdAndLeftAtIsNull(any());
		verify(hintService, never()).generateAutomatically(any(), any(), any());
	}

	@Test
	void 종료된_회의에서는_자동_힌트를_생성하지_않는다() {
		Meeting meeting = Mockito.mock(Meeting.class);
		when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
		when(meeting.getStatus()).thenReturn(MeetingStatus.SUMMARIZING);

		listener(60).generate(event(80, true));

		verify(participantRepository, never()).findByMeetingIdAndLeftAtIsNull(any());
		verify(hintService, never()).generateAutomatically(any(), any(), any());
	}

	private AutoHintGenerationListener listener(int threshold) {
		return new AutoHintGenerationListener(
				new AutoHintProperties(true, threshold), participantRepository, meetingRepository, hintService, eventPublisher,
				new SyncTaskExecutor());
	}

	private void inProgressMeeting() {
		Meeting meeting = Mockito.mock(Meeting.class);
		when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
		when(meeting.getStatus()).thenReturn(MeetingStatus.IN_PROGRESS);
	}

	private LiveContextUpdatedEvent event(int importance, boolean shouldGenerate) {
		return new LiveContextUpdatedEvent(
				1L,
				LiveContextSnapshot.empty(),
				3L,
				2,
				new AutoHintDecision(shouldGenerate, 3L, importance, "일정이 확정됨")
		);
	}
}
