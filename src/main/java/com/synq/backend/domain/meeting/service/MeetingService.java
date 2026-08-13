package com.synq.backend.domain.meeting.service;

import com.synq.backend.domain.meeting.code.MeetingErrorCode;
import com.synq.backend.domain.meeting.dto.MeetingListResponse;
import com.synq.backend.domain.meeting.dto.MeetingParticipantResponse;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.entity.MeetingParticipant;
import com.synq.backend.domain.meeting.entity.MeetingStatus;
import com.synq.backend.domain.meeting.entity.ParticipantRole;
import com.synq.backend.domain.meeting.event.MeetingEndedEvent;
import com.synq.backend.domain.meeting.event.MeetingPausedEvent;
import com.synq.backend.domain.meeting.event.MeetingResumedEvent;
import com.synq.backend.domain.meeting.port.MeetingSummaryTopicsReader;
import com.synq.backend.domain.meeting.port.MeetingSummaryJobCreator;
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
import java.util.UUID;
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
	private final MeetingSummaryJobCreator meetingSummaryJobCreator;
	private final MeetingParticipantAccessValidator participantAccessValidator;

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
				keyTopicsByMeetingId.get(meeting.getId()),
				meeting.isPaused(),
				meeting.activeSeconds()
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
	public MeetingEndResult end(Long meetingId, Long userId) {
		if (!meetingRepository.existsById(meetingId)) {
			throw new GeneralException(MeetingErrorCode.MEETING_NOT_FOUND);
		}
		if (!isHost(meetingId, userId)) {
			throw new GeneralException(MeetingErrorCode.NOT_HOST);
		}
		// 정상 /end 와 유예 타이머의 forceEndByDisconnect 가 동시에 들어와도 한쪽만 반영되도록
		// 원자적 조건부 UPDATE 로 상태를 전환한다.
		if (meetingRepository.endIfInProgress(meetingId) == 0) {
			throw new GeneralException(MeetingErrorCode.MEETING_ALREADY_ENDED);
		}

		Meeting meeting = meetingRepository.findById(meetingId)
				.orElseThrow(() -> new GeneralException(MeetingErrorCode.MEETING_NOT_FOUND));
		// 응답에서 상태 조회에 쓸 Job ID를 돌려주기 위해, 종료 트랜잭션 안에서 Job을 먼저 접수한다.
		// 실제 AI 호출은 커밋 뒤 이벤트 리스너가 시작하므로 /end 응답은 즉시 반환된다.
		UUID summaryJobId = meetingSummaryJobCreator.createQueuedJob(meeting.getId());
		eventPublisher.publishEvent(new MeetingEndedEvent(meeting.getId(), summaryJobId));
		return new MeetingEndResult(meeting, summaryJobId);
	}

	// 진행자 WS 연결이 끊긴 채 유예시간이 지나도 재연결이 없으면 시스템이 대신 회의를 종료한다.
	// 호출 주체가 시스템이라 호스트 인가 체크가 없고, 이미 종료된 회의면 조용히 넘어간다
	// (유예 타이머가 정상 /end 호출과 경합해도 예외를 던지지 않도록).
	@Transactional
	public void forceEndByDisconnect(Long meetingId) {
		if (meetingRepository.endIfInProgress(meetingId) > 0) {
			UUID summaryJobId = meetingSummaryJobCreator.createQueuedJob(meetingId);
			eventPublisher.publishEvent(new MeetingEndedEvent(meetingId, summaryJobId));
		}
	}

	public record MeetingEndResult(Meeting meeting, UUID summaryJobId) {
	}

	// 진행자(HOST)만 회의를 일시정지할 수 있다. 참여자 타이머 동기화를 위해 그 시점의 누적 활성
	// 시간(activeSeconds)을 이벤트에 실어 보낸다.
	@Transactional
	public Meeting pause(Long meetingId, Long userId) {
		Meeting meeting = meetingRepository.findById(meetingId)
				.orElseThrow(() -> new GeneralException(MeetingErrorCode.MEETING_NOT_FOUND));
		if (!isHost(meetingId, userId)) {
			throw new GeneralException(MeetingErrorCode.NOT_HOST_TO_PAUSE);
		}
		if (meeting.getStatus() != MeetingStatus.IN_PROGRESS) {
			throw new GeneralException(MeetingErrorCode.MEETING_ALREADY_ENDED);
		}
		if (meeting.isPaused()) {
			throw new GeneralException(MeetingErrorCode.MEETING_ALREADY_PAUSED);
		}

		meeting.pause();
		eventPublisher.publishEvent(new MeetingPausedEvent(meeting.getId(), meeting.activeSeconds()));
		return meeting;
	}

	// 진행자(HOST)만 회의를 재개할 수 있다.
	@Transactional
	public Meeting resume(Long meetingId, Long userId) {
		Meeting meeting = meetingRepository.findById(meetingId)
				.orElseThrow(() -> new GeneralException(MeetingErrorCode.MEETING_NOT_FOUND));
		if (!isHost(meetingId, userId)) {
			throw new GeneralException(MeetingErrorCode.NOT_HOST_TO_RESUME);
		}
		if (meeting.getStatus() != MeetingStatus.IN_PROGRESS) {
			throw new GeneralException(MeetingErrorCode.MEETING_ALREADY_ENDED);
		}
		if (!meeting.isPaused()) {
			throw new GeneralException(MeetingErrorCode.MEETING_NOT_PAUSED);
		}

		meeting.resume();
		eventPublisher.publishEvent(new MeetingResumedEvent(meeting.getId(), meeting.activeSeconds()));
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

	// 현재 활성 참여자(leftAt이 null인 사람)만 반환한다. 나갔다가 다시 들어올 수 있으므로
	// 나간 사람은 목록에서 빠지고, 연결이 순간적으로 끊겨도 leave API를 부르지 않는 한 남아있는다.
	@Transactional(readOnly = true)
	public List<MeetingParticipantResponse> findParticipants(Long meetingId, Long userId) {
		participantAccessValidator.validateActiveParticipant(meetingId, userId);

		List<MeetingParticipant> participants =
				meetingParticipantRepository.findByMeetingIdAndLeftAtIsNull(meetingId);
		Map<Long, User> userById = userRepository
				.findAllById(participants.stream().map(MeetingParticipant::getUserId).toList()).stream()
				.collect(Collectors.toMap(User::getUserId, Function.identity()));

		return participants.stream()
				.map(participant -> toParticipantResponse(participant, userById))
				.toList();
	}

	private MeetingParticipantResponse toParticipantResponse(
			MeetingParticipant participant, Map<Long, User> userById) {
		User user = userById.get(participant.getUserId());
		return new MeetingParticipantResponse(
				participant.getUserId(),
				user != null ? user.getName() : null,
				user != null ? profileImageService.toUrl(user.getProfileImageKey()) : null,
				participant.getRole().name(),
				participant.getJoinedAt());
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
