package com.synq.backend.domain.meeting.service;

import com.synq.backend.domain.meeting.code.MeetingErrorCode;
import com.synq.backend.domain.meeting.dto.MeetingListResponse;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.entity.MeetingParticipant;
import com.synq.backend.domain.meeting.entity.MeetingStatus;
import com.synq.backend.domain.meeting.entity.ParticipantRole;
import com.synq.backend.domain.meeting.event.MeetingEndedEvent;
import com.synq.backend.domain.meeting.port.MeetingSummaryTopicsReader;
import com.synq.backend.domain.meeting.port.ProjectMembershipChecker;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.domain.user.service.ProfileImageService;
import com.synq.backend.global.apipayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeetingService {

	private static final DateTimeFormatter TEMP_TITLE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

	private final MeetingRepository meetingRepository;
	private final MeetingParticipantRepository meetingParticipantRepository;
	private final ProjectMembershipChecker projectMembershipChecker;
	private final ApplicationEventPublisher eventPublisher;
	private final UserRepository userRepository;
	private final ProfileImageService profileImageService;
	private final MeetingSummaryTopicsReader meetingSummaryTopicsReader;

	@Transactional
	public Meeting create(Long projectId, Long userId, Boolean consentAgreed) {
		if (consentAgreed == null || !consentAgreed) {
			throw new GeneralException(MeetingErrorCode.CONSENT_REQUIRED);
		}
		if (!projectMembershipChecker.isMember(projectId, userId)) {
			throw new GeneralException(MeetingErrorCode.NOT_PROJECT_MEMBER);
		}
		// 프로젝트당 진행 중인 회의는 동시에 하나만 존재할 수 있다. 아래 존재 체크는 흔한 경우를 빠르게
		// 막기 위한 것일 뿐이고(TOCTOU 레이스로 뚫릴 수 있음), 실제 보장은 DB의 partial unique index
		// (uq_meeting_project_active)가 한다 — 동시 요청 중 뒤에 커밋되는 쪽은 저장 시점에 제약 위반으로 막힌다.
		if (meetingRepository.existsByProjectIdAndStatus(projectId, MeetingStatus.IN_PROGRESS)) {
			throw new GeneralException(MeetingErrorCode.CONCURRENT_MEETING_EXISTS);
		}

		Meeting meeting;
		try {
			meeting = meetingRepository.save(Meeting.of(projectId, temporaryTitle()));
		} catch (DataIntegrityViolationException e) {
			throw new GeneralException(MeetingErrorCode.CONCURRENT_MEETING_EXISTS);
		}
		meetingParticipantRepository.save(MeetingParticipant.of(meeting.getId(), userId, ParticipantRole.HOST));
		return meeting;
	}

	// 프로젝트의 회의 목록을 최근 생성순으로 조회한다. 호스트 정보/주제 태그는 N+1을 피하기 위해
	// meetingId 단위로 배치 조회한 뒤 메모리에서 조합한다.
	@Transactional(readOnly = true)
	public List<MeetingListResponse> findAll(Long projectId, Long userId) {
		if (!projectMembershipChecker.isMember(projectId, userId)) {
			throw new GeneralException(MeetingErrorCode.NOT_PROJECT_MEMBER_TO_LIST);
		}

		List<Meeting> meetings = meetingRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
		List<Long> meetingIds = meetings.stream().map(Meeting::getId).toList();

		Map<Long, Long> hostUserIdByMeetingId = meetingParticipantRepository
				.findByMeetingIdInAndRole(meetingIds, ParticipantRole.HOST).stream()
				.collect(Collectors.toMap(
						MeetingParticipant::getMeetingId, MeetingParticipant::getUserId, (a, b) -> a));
		Map<Long, User> userById = userRepository.findAllById(hostUserIdByMeetingId.values()).stream()
				.collect(Collectors.toMap(User::getUserId, Function.identity()));
		Map<Long, List<String>> keyTopicsByMeetingId = meetingSummaryTopicsReader.findKeyTopicsByMeetingIds(meetingIds);

		return meetings.stream()
				.map(meeting -> toListResponse(meeting, hostUserIdByMeetingId, userById, keyTopicsByMeetingId))
				.toList();
	}

	private MeetingListResponse toListResponse(
			Meeting meeting,
			Map<Long, Long> hostUserIdByMeetingId,
			Map<Long, User> userById,
			Map<Long, List<String>> keyTopicsByMeetingId
	) {
		MeetingListResponse.Host host = Optional.ofNullable(hostUserIdByMeetingId.get(meeting.getId()))
				.map(userById::get)
				.map(user -> new MeetingListResponse.Host(
						user.getUserId(), user.getName(), profileImageService.toUrl(user.getProfileImageKey())))
				.orElse(null);

		return new MeetingListResponse(
				meeting.getId(),
				meeting.getTitle(),
				meeting.getStatus().name(),
				meeting.getCreatedAt(),
				durationSeconds(meeting),
				host,
				keyTopicsByMeetingId.get(meeting.getId())
		);
	}

	// 회의가 아직 끝나지 않았으면(진행 중) 길이를 알 수 없으므로 null.
	private Long durationSeconds(Meeting meeting) {
		if (meeting.getEndedAt() == null) {
			return null;
		}
		return Duration.between(meeting.getStartedAt(), meeting.getEndedAt()).toSeconds();
	}

	// 진행자(회의 HOST)만 회의를 종료할 수 있다. 종료 즉시 SUMMARIZING 으로 전환하고
	// MeetingEndedEvent 를 발행하면, ai.summary 도메인이 이를 수신해 AI 정리를 비동기로 시작한다.
	@Transactional
	public Meeting end(Long meetingId, Long userId) {
		Meeting meeting = meetingRepository.findById(meetingId)
				.orElseThrow(() -> new GeneralException(MeetingErrorCode.MEETING_NOT_FOUND));
		if (!isHost(meetingId, userId)) {
			throw new GeneralException(MeetingErrorCode.NOT_HOST);
		}
		if (meeting.getStatus() != MeetingStatus.IN_PROGRESS) {
			throw new GeneralException(MeetingErrorCode.MEETING_ALREADY_ENDED);
		}

		meeting.end();
		// 커밋 이후 리스너가 요약을 시작하도록, 상태 저장이 확정된 뒤에 이벤트가 처리된다.
		eventPublisher.publishEvent(new MeetingEndedEvent(meeting.getId()));
		return meeting;
	}

	// 진행자(HOST)만 회의를 삭제할 수 있다. 진행 중인 회의는 삭제할 수 없고 먼저 종료해야 한다.
	// 자식 데이터(참여자/전사/요약 등)는 meeting FK 의 ON DELETE CASCADE 가 처리한다.
	@Transactional
	public void delete(Long meetingId, Long userId) {
		Meeting meeting = meetingRepository.findById(meetingId)
				.orElseThrow(() -> new GeneralException(MeetingErrorCode.MEETING_NOT_FOUND));
		if (!isHost(meetingId, userId)) {
			throw new GeneralException(MeetingErrorCode.NOT_HOST_TO_DELETE);
		}
		if (meeting.getStatus() == MeetingStatus.IN_PROGRESS) {
			throw new GeneralException(MeetingErrorCode.CANNOT_DELETE_IN_PROGRESS_MEETING);
		}

		meetingRepository.delete(meeting);
	}

	// 현재 활성 참여자만 나갈 수 있고, 호스트는 나갈 수 없다(종료/삭제로만 회의를 정리할 수 있다).
	// 나간 뒤 재참여는 기존 join() 이 처리한다(새 MEMBER row 생성).
	@Transactional
	public void leave(Long meetingId, Long userId) {
		if (!meetingRepository.existsById(meetingId)) {
			throw new GeneralException(MeetingErrorCode.MEETING_NOT_FOUND);
		}
		MeetingParticipant participant = meetingParticipantRepository
				.findByMeetingIdAndUserIdAndLeftAtIsNull(meetingId, userId)
				.orElseThrow(() -> new GeneralException(MeetingErrorCode.NOT_MEETING_PARTICIPANT));
		if (participant.getRole() == ParticipantRole.HOST) {
			throw new GeneralException(MeetingErrorCode.HOST_CANNOT_LEAVE);
		}

		participant.leave();
	}

	// 회의 진행자(HOST) 판별. 회의 시작 시 생성자를 role=HOST 로 저장해두므로 그 참여자인지 확인한다.
	private boolean isHost(Long meetingId, Long userId) {
		return meetingParticipantRepository.findByMeetingIdAndUserId(meetingId, userId).stream()
				.anyMatch(participant -> participant.getRole() == ParticipantRole.HOST);
	}

	// 진행 중(IN_PROGRESS)인 회의만 참여할 수 있다 — 종료된 회의는 이미 참여했던 사람이라도 다시 들어올 수 없다.
	// 그 다음에야 이미 참여 중인 유저가 다시 호출해도(새로고침/중복 탭) 에러 대신 기존 참여 정보를 그대로 반환한다.
	// join으로 들어오는 참여자는 항상 MEMBER이며, HOST는 회의 생성 시점에만 부여된다.
	@Transactional
	public MeetingJoinResult join(Long meetingId, Long userId) {
		Meeting meeting = meetingRepository.findById(meetingId)
				.orElseThrow(() -> new GeneralException(MeetingErrorCode.MEETING_NOT_FOUND));

		if (meeting.getStatus() != MeetingStatus.IN_PROGRESS) {
			throw new GeneralException(MeetingErrorCode.MEETING_ALREADY_ENDED);
		}

		Optional<MeetingParticipant> existing =
				meetingParticipantRepository.findByMeetingIdAndUserIdAndLeftAtIsNull(meetingId, userId);
		if (existing.isPresent()) {
			return new MeetingJoinResult(meeting, existing.get());
		}

		if (!projectMembershipChecker.isMember(meeting.getProjectId(), userId)) {
			throw new GeneralException(MeetingErrorCode.NOT_PROJECT_MEMBER_TO_JOIN);
		}

		MeetingParticipant participant = meetingParticipantRepository.save(
				MeetingParticipant.of(meetingId, userId, ParticipantRole.MEMBER));
		return new MeetingJoinResult(meeting, participant);
	}

	// AI 정리 완료/실패 이벤트를 받아 회의 상태를 확정하는 진입점.
	@Transactional
	public void markSummarized(Long meetingId) {
		meetingRepository.findById(meetingId).ifPresent(Meeting::markSummarized);
	}

	@Transactional
	public void markSummaryFailed(Long meetingId) {
		meetingRepository.findById(meetingId).ifPresent(Meeting::markSummaryFailed);
	}

	// 회의 진행자(HOST)만 제목을 수정할 수 있고, 회의 상태와 무관하게(진행 중/종료 후 모두) 허용한다.
	@Transactional
	public Meeting updateTitle(Long meetingId, Long userId, String title) {
		if (title == null || title.isBlank()) {
			throw new GeneralException(MeetingErrorCode.BLANK_TITLE);
		}

		Meeting meeting = meetingRepository.findById(meetingId)
				.orElseThrow(() -> new GeneralException(MeetingErrorCode.MEETING_NOT_FOUND));
		if (!isHost(meetingId, userId)) {
			throw new GeneralException(MeetingErrorCode.NOT_HOST_TO_UPDATE_TITLE);
		}

		meeting.updateTitle(title);
		return meeting;
	}

	// 종료 시 AI가 전사 기반으로 덮어쓰기 전까지의 임시 제목. 사용자가 직접 수정한 적 있으면
	// (제목 수정 API 이슈에서 추가될 플래그 기준) 종료 시점에도 이 값을 덮어쓰지 않는다.
	private String temporaryTitle() {
		return "%s 회의".formatted(LocalDate.now().format(TEMP_TITLE_DATE_FORMAT));
	}
}
