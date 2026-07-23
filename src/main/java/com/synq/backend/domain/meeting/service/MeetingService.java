package com.synq.backend.domain.meeting.service;

import com.synq.backend.domain.meeting.code.MeetingErrorCode;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.entity.MeetingParticipant;
import com.synq.backend.domain.meeting.entity.MeetingStatus;
import com.synq.backend.domain.meeting.entity.ParticipantRole;
import com.synq.backend.domain.meeting.event.MeetingEndedEvent;
import com.synq.backend.domain.meeting.port.ProjectMembershipChecker;
import com.synq.backend.domain.meeting.port.ProjectOwnerChecker;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.global.apipayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class MeetingService {

	private static final DateTimeFormatter TEMP_TITLE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

	private final MeetingRepository meetingRepository;
	private final MeetingParticipantRepository meetingParticipantRepository;
	private final ProjectMembershipChecker projectMembershipChecker;
	private final ProjectOwnerChecker projectOwnerChecker;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public Meeting create(Long projectId, Long userId, Boolean consentAgreed) {
		if (consentAgreed == null || !consentAgreed) {
			throw new GeneralException(MeetingErrorCode.CONSENT_REQUIRED);
		}
		if (!projectMembershipChecker.isMember(projectId, userId)) {
			throw new GeneralException(MeetingErrorCode.NOT_PROJECT_MEMBER);
		}

		Meeting meeting = meetingRepository.save(Meeting.of(projectId, temporaryTitle()));
		meetingParticipantRepository.save(MeetingParticipant.of(meeting.getId(), userId, ParticipantRole.HOST));
		return meeting;
	}

	// 진행자(프로젝트 소유자)만 회의를 종료할 수 있다. 종료 즉시 SUMMARIZING 으로 전환하고
	// MeetingEndedEvent 를 발행하면, ai.summary 도메인이 이를 수신해 AI 정리를 비동기로 시작한다.
	@Transactional
	public Meeting end(Long meetingId, Long userId) {
		Meeting meeting = meetingRepository.findById(meetingId)
				.orElseThrow(() -> new GeneralException(MeetingErrorCode.MEETING_NOT_FOUND));
		if (!projectOwnerChecker.isOwner(meeting.getProjectId(), userId)) {
			throw new GeneralException(MeetingErrorCode.NOT_PROJECT_OWNER);
		}
		if (meeting.getStatus() != MeetingStatus.IN_PROGRESS) {
			throw new GeneralException(MeetingErrorCode.MEETING_ALREADY_ENDED);
		}

		meeting.end();
		// 커밋 이후 리스너가 요약을 시작하도록, 상태 저장이 확정된 뒤에 이벤트가 처리된다.
		eventPublisher.publishEvent(new MeetingEndedEvent(meeting.getId()));
		return meeting;
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

	// 종료 시 AI가 전사 기반으로 덮어쓰기 전까지의 임시 제목. 사용자가 직접 수정한 적 있으면
	// (제목 수정 API 이슈에서 추가될 플래그 기준) 종료 시점에도 이 값을 덮어쓰지 않는다.
	private String temporaryTitle() {
		return "%s 회의".formatted(LocalDate.now().format(TEMP_TITLE_DATE_FORMAT));
	}
}
