package com.synq.backend.domain.ai.summary.application;

import com.synq.backend.domain.ai.summary.code.SummaryErrorCode;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.global.apipayload.exception.GeneralException;
import org.springframework.stereotype.Component;

/**
 * 회의 후 기능은 퇴장 여부와 관계없이 실제 회의 참여 이력이 있는 사용자에게만 허용한다.
 */
@Component
public class SummaryAccessValidator {

	private final MeetingParticipantRepository meetingParticipantRepository;

	public SummaryAccessValidator(MeetingParticipantRepository meetingParticipantRepository) {
		this.meetingParticipantRepository = meetingParticipantRepository;
	}

	public void validateParticipant(Long meetingId, Long userId) {
		if (!meetingParticipantRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
			throw new GeneralException(SummaryErrorCode.NOT_MEETING_PARTICIPANT);
		}
	}
}
