package com.synq.backend.domain.meeting.service;

import com.synq.backend.domain.meeting.code.MeetingErrorCode;
import com.synq.backend.domain.meeting.dto.MeetingListResponse;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.entity.MeetingParticipant;
import com.synq.backend.domain.meeting.entity.MeetingStatus;
import com.synq.backend.domain.meeting.entity.ParticipantRole;
import com.synq.backend.domain.meeting.port.MeetingSummaryTopicsReader;
import com.synq.backend.domain.meeting.port.ProjectMembershipChecker;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.domain.user.service.ProfileImageService;
import com.synq.backend.global.apipayload.exception.GeneralException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MeetingServiceTest {

	private final MeetingRepository meetingRepository = mock(MeetingRepository.class);
	private final MeetingParticipantRepository meetingParticipantRepository =
			mock(MeetingParticipantRepository.class);
	private final ProjectMembershipChecker projectMembershipChecker =
			mock(ProjectMembershipChecker.class);
	private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final ProfileImageService profileImageService = mock(ProfileImageService.class);
	private final MeetingSummaryTopicsReader meetingSummaryTopicsReader = mock(MeetingSummaryTopicsReader.class);
	private final MeetingService meetingService = new MeetingService(
			meetingRepository,
			meetingParticipantRepository,
			projectMembershipChecker,
			eventPublisher,
			userRepository,
			profileImageService,
			meetingSummaryTopicsReader
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

	@Test
	void 이미_진행_중인_회의가_있으면_생성시_CONCURRENT_MEETING_EXISTS_예외를_발생시킨다() {
		when(projectMembershipChecker.isMember(1L, 10L)).thenReturn(true);
		when(meetingRepository.existsByProjectIdAndStatus(1L, MeetingStatus.IN_PROGRESS)).thenReturn(true);

		assertThatThrownBy(() -> meetingService.create(1L, 10L, true))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(MeetingErrorCode.CONCURRENT_MEETING_EXISTS));
		verifyNoInteractions(meetingParticipantRepository, eventPublisher);
	}

	// 존재 체크(existsByProjectIdAndStatus)를 둘 다 통과한 뒤에도, save() 시점에 DB의
	// partial unique index(uq_meeting_project_active)가 동시 요청의 레이스를 막아준다.
	// 이 경우 DataIntegrityViolationException을 CONCURRENT_MEETING_EXISTS로 변환해야 한다.
	@Test
	void 존재체크_통과후_저장시_유니크_제약_위반이면_CONCURRENT_MEETING_EXISTS_예외를_발생시킨다() {
		when(projectMembershipChecker.isMember(1L, 10L)).thenReturn(true);
		when(meetingRepository.existsByProjectIdAndStatus(1L, MeetingStatus.IN_PROGRESS)).thenReturn(false);
		when(meetingRepository.save(any(Meeting.class)))
				.thenThrow(new DataIntegrityViolationException("uq_meeting_project_active"));

		assertThatThrownBy(() -> meetingService.create(1L, 10L, true))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(MeetingErrorCode.CONCURRENT_MEETING_EXISTS));
		verifyNoInteractions(meetingParticipantRepository, eventPublisher);
	}

	// AlwaysMemberProjectMembershipChecker(test 프로필)는 항상 true라 이 분기는 통합 테스트로는
	// 도달 불가능하다. 프로젝트 비멤버 케이스는 여기서 mock으로 직접 검증한다.
	@Test
	void 참여시_기존_참여자가_아니고_프로젝트_멤버도_아니면_NOT_PROJECT_MEMBER_TO_JOIN_예외를_발생시킨다() {
		Meeting meeting = Meeting.of(1L, "회의");
		when(meetingRepository.findById(5L)).thenReturn(Optional.of(meeting));
		when(meetingParticipantRepository.findByMeetingIdAndUserIdAndLeftAtIsNull(5L, 10L))
				.thenReturn(Optional.empty());
		when(projectMembershipChecker.isMember(1L, 10L)).thenReturn(false);

		assertThatThrownBy(() -> meetingService.join(5L, 10L))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(MeetingErrorCode.NOT_PROJECT_MEMBER_TO_JOIN));
	}

	@Test
	void 진행자가_제목을_수정하면_title이_바뀌고_titleAutoGenerated가_false가_된다() {
		Meeting meeting = Meeting.of(1L, "회의");
		when(meetingRepository.findById(5L)).thenReturn(Optional.of(meeting));
		when(meetingParticipantRepository.findByMeetingIdAndUserId(5L, 10L))
				.thenReturn(List.of(MeetingParticipant.of(5L, 10L, ParticipantRole.HOST)));

		Meeting updated = meetingService.updateTitle(5L, 10L, "새 회의 제목");

		assertThat(updated.getTitle()).isEqualTo("새 회의 제목");
		assertThat(updated.isTitleAutoGenerated()).isFalse();
	}

	@Test
	void 종료된_회의도_제목을_수정할_수_있다() {
		Meeting meeting = Meeting.of(1L, "회의");
		meeting.end();
		when(meetingRepository.findById(5L)).thenReturn(Optional.of(meeting));
		when(meetingParticipantRepository.findByMeetingIdAndUserId(5L, 10L))
				.thenReturn(List.of(MeetingParticipant.of(5L, 10L, ParticipantRole.HOST)));

		Meeting updated = meetingService.updateTitle(5L, 10L, "종료 후 수정한 제목");

		assertThat(updated.getTitle()).isEqualTo("종료 후 수정한 제목");
	}

	@Test
	void 진행자가_아니면_제목_수정시_NOT_HOST_TO_UPDATE_TITLE_예외를_발생시킨다() {
		Meeting meeting = Meeting.of(1L, "회의");
		when(meetingRepository.findById(5L)).thenReturn(Optional.of(meeting));
		when(meetingParticipantRepository.findByMeetingIdAndUserId(5L, 10L))
				.thenReturn(List.of(MeetingParticipant.of(5L, 10L, ParticipantRole.MEMBER)));

		assertThatThrownBy(() -> meetingService.updateTitle(5L, 10L, "새 회의 제목"))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(MeetingErrorCode.NOT_HOST_TO_UPDATE_TITLE));
	}

	@Test
	void 존재하지_않는_회의면_제목_수정시_MEETING_NOT_FOUND_예외를_발생시킨다() {
		when(meetingRepository.findById(5L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> meetingService.updateTitle(5L, 10L, "새 회의 제목"))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(MeetingErrorCode.MEETING_NOT_FOUND));
	}

	@Test
	void 제목이_공백이면_BLANK_TITLE_예외를_발생시킨다() {
		assertThatThrownBy(() -> meetingService.updateTitle(5L, 10L, "   "))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(MeetingErrorCode.BLANK_TITLE));
		verifyNoInteractions(meetingRepository);
	}

	@Test
	void 진행자가_종료된_회의를_삭제하면_meetingRepository_delete가_호출된다() {
		Meeting meeting = Meeting.of(1L, "회의");
		meeting.end();
		when(meetingRepository.findById(5L)).thenReturn(Optional.of(meeting));
		when(meetingParticipantRepository.findByMeetingIdAndUserId(5L, 10L))
				.thenReturn(List.of(MeetingParticipant.of(5L, 10L, ParticipantRole.HOST)));

		meetingService.delete(5L, 10L);

		verify(meetingRepository).delete(meeting);
	}

	@Test
	void 진행_중인_회의를_삭제하려_하면_CANNOT_DELETE_IN_PROGRESS_MEETING_예외를_발생시킨다() {
		Meeting meeting = Meeting.of(1L, "회의");
		when(meetingRepository.findById(5L)).thenReturn(Optional.of(meeting));
		when(meetingParticipantRepository.findByMeetingIdAndUserId(5L, 10L))
				.thenReturn(List.of(MeetingParticipant.of(5L, 10L, ParticipantRole.HOST)));

		assertThatThrownBy(() -> meetingService.delete(5L, 10L))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(MeetingErrorCode.CANNOT_DELETE_IN_PROGRESS_MEETING));
		verify(meetingRepository, never()).delete(meeting);
	}

	@Test
	void 진행자가_아니면_삭제시_NOT_HOST_TO_DELETE_예외를_발생시킨다() {
		Meeting meeting = Meeting.of(1L, "회의");
		meeting.end();
		when(meetingRepository.findById(5L)).thenReturn(Optional.of(meeting));
		when(meetingParticipantRepository.findByMeetingIdAndUserId(5L, 10L))
				.thenReturn(List.of(MeetingParticipant.of(5L, 10L, ParticipantRole.MEMBER)));

		assertThatThrownBy(() -> meetingService.delete(5L, 10L))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(MeetingErrorCode.NOT_HOST_TO_DELETE));
	}

	@Test
	void 존재하지_않는_회의면_삭제시_MEETING_NOT_FOUND_예외를_발생시킨다() {
		when(meetingRepository.findById(5L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> meetingService.delete(5L, 10L))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(MeetingErrorCode.MEETING_NOT_FOUND));
	}

	@Test
	void 활성_참여자가_나가면_leave가_호출된다() {
		MeetingParticipant participant = MeetingParticipant.of(5L, 10L, ParticipantRole.MEMBER);
		when(meetingRepository.existsById(5L)).thenReturn(true);
		when(meetingParticipantRepository.findByMeetingIdAndUserIdAndLeftAtIsNull(5L, 10L))
				.thenReturn(Optional.of(participant));

		meetingService.leave(5L, 10L);

		assertThat(participant.getLeftAt()).isNotNull();
	}

	@Test
	void 호스트가_나가려_하면_HOST_CANNOT_LEAVE_예외를_발생시킨다() {
		MeetingParticipant participant = MeetingParticipant.of(5L, 10L, ParticipantRole.HOST);
		when(meetingRepository.existsById(5L)).thenReturn(true);
		when(meetingParticipantRepository.findByMeetingIdAndUserIdAndLeftAtIsNull(5L, 10L))
				.thenReturn(Optional.of(participant));

		assertThatThrownBy(() -> meetingService.leave(5L, 10L))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(MeetingErrorCode.HOST_CANNOT_LEAVE));
		assertThat(participant.getLeftAt()).isNull();
	}

	@Test
	void 활성_참여자가_아니면_나가기시_NOT_MEETING_PARTICIPANT_예외를_발생시킨다() {
		when(meetingRepository.existsById(5L)).thenReturn(true);
		when(meetingParticipantRepository.findByMeetingIdAndUserIdAndLeftAtIsNull(5L, 10L))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> meetingService.leave(5L, 10L))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(MeetingErrorCode.NOT_MEETING_PARTICIPANT));
	}

	@Test
	void 존재하지_않는_회의면_나가기시_MEETING_NOT_FOUND_예외를_발생시킨다() {
		when(meetingRepository.existsById(5L)).thenReturn(false);

		assertThatThrownBy(() -> meetingService.leave(5L, 10L))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(MeetingErrorCode.MEETING_NOT_FOUND));
	}

	@Test
	void 프로젝트_멤버가_아니면_목록_조회시_NOT_PROJECT_MEMBER_TO_LIST_예외를_발생시킨다() {
		when(projectMembershipChecker.isMember(1L, 10L)).thenReturn(false);

		assertThatThrownBy(() -> meetingService.findAll(1L, 10L))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(MeetingErrorCode.NOT_PROJECT_MEMBER_TO_LIST));
		verifyNoInteractions(meetingRepository);
	}

	@Test
	void 목록_조회시_호스트와_주제태그와_회의길이를_함께_조합한다() {
		Meeting meeting = Meeting.of(1L, "회의");
		meeting.end();
		ReflectionTestUtils.setField(meeting, "id", 100L);

		when(projectMembershipChecker.isMember(1L, 10L)).thenReturn(true);
		when(meetingRepository.findByProjectIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(meeting));

		MeetingParticipant host = MeetingParticipant.of(100L, 20L, ParticipantRole.HOST);
		when(meetingParticipantRepository.findByMeetingIdInAndRole(List.of(100L), ParticipantRole.HOST))
				.thenReturn(List.of(host));

		User hostUser = User.ofLocal("호스트", "host@synq.com", "password-hash");
		ReflectionTestUtils.setField(hostUser, "userId", 20L);
		when(userRepository.findAllById(any())).thenReturn(List.of(hostUser));

		when(meetingSummaryTopicsReader.findKeyTopicsByMeetingIds(List.of(100L)))
				.thenReturn(Map.of(100L, List.of("근황토크", "일정조율")));

		List<MeetingListResponse> result = meetingService.findAll(1L, 10L);

		assertThat(result).hasSize(1);
		MeetingListResponse response = result.get(0);
		assertThat(response.meetingId()).isEqualTo(100L);
		assertThat(response.host().userId()).isEqualTo(20L);
		assertThat(response.host().name()).isEqualTo("호스트");
		assertThat(response.keyTopics()).containsExactly("근황토크", "일정조율");
		assertThat(response.durationSeconds()).isNotNull();
	}

	@Test
	void 요약이_없으면_keyTopics는_null이고_진행중이면_durationSeconds도_null이다() {
		Meeting meeting = Meeting.of(1L, "회의");
		ReflectionTestUtils.setField(meeting, "id", 200L);

		when(projectMembershipChecker.isMember(1L, 10L)).thenReturn(true);
		when(meetingRepository.findByProjectIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(meeting));
		when(meetingParticipantRepository.findByMeetingIdInAndRole(List.of(200L), ParticipantRole.HOST))
				.thenReturn(List.of());
		when(userRepository.findAllById(any())).thenReturn(List.of());
		when(meetingSummaryTopicsReader.findKeyTopicsByMeetingIds(List.of(200L)))
				.thenReturn(Map.of());

		MeetingListResponse response = meetingService.findAll(1L, 10L).get(0);

		assertThat(response.keyTopics()).isNull();
		assertThat(response.durationSeconds()).isNull();
		assertThat(response.host()).isNull();
	}
}
