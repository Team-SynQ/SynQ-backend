package com.synq.backend.domain.meeting.service;

import com.synq.backend.domain.meeting.code.MeetingErrorCode;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.entity.MeetingParticipant;
import com.synq.backend.domain.meeting.entity.ParticipantRole;
import com.synq.backend.domain.meeting.port.ProjectMembershipChecker;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.global.apipayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
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

	// 종료 시 AI가 전사 기반으로 덮어쓰기 전까지의 임시 제목. 사용자가 직접 수정한 적 있으면
	// (제목 수정 API 이슈에서 추가될 플래그 기준) 종료 시점에도 이 값을 덮어쓰지 않는다.
	private String temporaryTitle() {
		return "%s 회의".formatted(LocalDate.now().format(TEMP_TITLE_DATE_FORMAT));
	}
}
