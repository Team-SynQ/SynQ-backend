package com.synq.backend.domain.meeting.service;

import com.synq.backend.domain.meeting.code.MeetingErrorCode;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.global.apipayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 회의 중 기능에서 현재 참여 중인 사용자인지 확인한다. */
@Component
@RequiredArgsConstructor
public class MeetingParticipantAccessValidator {

	private final MeetingRepository meetingRepository;
	private final MeetingParticipantRepository meetingParticipantRepository;

	public void validateActiveParticipant(Long meetingId, Long userId) {
		if (!meetingRepository.existsById(meetingId)) {
			throw new GeneralException(MeetingErrorCode.MEETING_NOT_FOUND);
		}
		if (!meetingParticipantRepository.existsByMeetingIdAndUserIdAndLeftAtIsNull(meetingId, userId)) {
			throw new GeneralException(MeetingErrorCode.NOT_MEETING_PARTICIPANT);
		}
	}
}
