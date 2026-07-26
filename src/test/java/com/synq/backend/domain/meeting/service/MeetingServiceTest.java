package com.synq.backend.domain.meeting.service;

import com.synq.backend.domain.meeting.code.MeetingErrorCode;
import com.synq.backend.domain.meeting.port.ProjectMembershipChecker;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.global.apipayload.exception.GeneralException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MeetingServiceTest {

	private final MeetingRepository meetingRepository = mock(MeetingRepository.class);
	private final MeetingParticipantRepository meetingParticipantRepository =
			mock(MeetingParticipantRepository.class);
	private final ProjectMembershipChecker projectMembershipChecker =
			mock(ProjectMembershipChecker.class);
	private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
	private final MeetingService meetingService = new MeetingService(
			meetingRepository,
			meetingParticipantRepository,
			projectMembershipChecker,
			eventPublisher
	);

	@Test
	void 프로젝트_멤버가_아니면_기존_NOT_PROJECT_MEMBER_예외를_발생시킨다() {
		when(projectMembershipChecker.isMember(1L, 10L)).thenReturn(false);

		assertThatThrownBy(() -> meetingService.create(1L, 10L, true))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(MeetingErrorCode.NOT_PROJECT_MEMBER));
		verifyNoInteractions(meetingRepository, meetingParticipantRepository, eventPublisher);
	}
}
