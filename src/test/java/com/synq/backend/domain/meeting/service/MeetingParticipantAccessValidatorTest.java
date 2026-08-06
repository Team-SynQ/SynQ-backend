package com.synq.backend.domain.meeting.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;

import com.synq.backend.domain.meeting.code.MeetingErrorCode;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.global.apipayload.exception.GeneralException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MeetingParticipantAccessValidatorTest {

	private final MeetingRepository meetingRepository = Mockito.mock(MeetingRepository.class);
	private final MeetingParticipantRepository participantRepository = Mockito.mock(MeetingParticipantRepository.class);
	private final MeetingParticipantAccessValidator validator = new MeetingParticipantAccessValidator(
			meetingRepository, participantRepository);

	@Test
	void 회의가_없으면_404를_반환한다() {
		given(meetingRepository.existsById(1L)).willReturn(false);

		assertThatThrownBy(() -> validator.validateActiveParticipant(1L, 10L))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> org.assertj.core.api.Assertions.assertThat(exception.getCode())
								.isEqualTo(MeetingErrorCode.MEETING_NOT_FOUND));
	}

	@Test
	void 활성_참여자가_아니면_403을_반환한다() {
		given(meetingRepository.existsById(1L)).willReturn(true);
		given(participantRepository.existsByMeetingIdAndUserIdAndLeftAtIsNull(1L, 10L)).willReturn(false);

		assertThatThrownBy(() -> validator.validateActiveParticipant(1L, 10L))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> org.assertj.core.api.Assertions.assertThat(exception.getCode())
								.isEqualTo(MeetingErrorCode.NOT_MEETING_PARTICIPANT));
	}

	@Test
	void 활성_참여자면_검증을_통과한다() {
		given(meetingRepository.existsById(1L)).willReturn(true);
		given(participantRepository.existsByMeetingIdAndUserIdAndLeftAtIsNull(1L, 10L)).willReturn(true);

		assertThatCode(() -> validator.validateActiveParticipant(1L, 10L)).doesNotThrowAnyException();
	}
}
