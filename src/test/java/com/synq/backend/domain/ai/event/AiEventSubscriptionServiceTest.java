package com.synq.backend.domain.ai.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.global.apipayload.exception.GeneralException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.task.SyncTaskExecutor;

class AiEventSubscriptionServiceTest {

	private final MeetingRepository meetingRepository = Mockito.mock(MeetingRepository.class);
	private final MeetingParticipantRepository participantRepository = Mockito.mock(MeetingParticipantRepository.class);
	private final AiEventSseProperties properties = new AiEventSseProperties(
			Duration.ofMinutes(30), Duration.ofSeconds(20), 100);
	private final AiEventSseEmitterRegistry emitterRegistry = new AiEventSseEmitterRegistry(
			new SyncTaskExecutor(), properties);
	private final AiEventSubscriptionService service = new AiEventSubscriptionService(
			meetingRepository,
			participantRepository,
			emitterRegistry,
			properties
	);

	@Test
	void 현재_회의_참여자만_SSE_연결을_생성한다() {
		when(meetingRepository.existsById(1L)).thenReturn(true);
		when(participantRepository.existsByMeetingIdAndUserIdAndLeftAtIsNull(1L, 10L)).thenReturn(true);

		service.subscribe(1L, 10L);

		assertThat(emitterRegistry.activeConnectionCount(1L)).isEqualTo(1);
	}

	@Test
	void 퇴장했거나_참여하지_않은_사용자는_구독할_수_없다() {
		when(meetingRepository.existsById(1L)).thenReturn(true);
		when(participantRepository.existsByMeetingIdAndUserIdAndLeftAtIsNull(1L, 10L)).thenReturn(false);

		assertThatThrownBy(() -> service.subscribe(1L, 10L))
				.isInstanceOf(GeneralException.class)
				.hasMessageContaining("회의 참여자만 AI 결과를 구독할 수 있습니다.");

		assertThat(emitterRegistry.activeConnectionCount(1L)).isZero();
	}
}
