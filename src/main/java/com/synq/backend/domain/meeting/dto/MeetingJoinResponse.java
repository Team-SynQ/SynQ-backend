package com.synq.backend.domain.meeting.dto;

import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.entity.MeetingParticipant;
import com.synq.backend.domain.meeting.service.MeetingJoinResult;

import java.time.LocalDateTime;

public record MeetingJoinResponse(
		Long meetingId,
		String title,
		String status,
		String role,
		LocalDateTime joinedAt,
		LocalDateTime startedAt,
		String wsUrl
) {
	public static MeetingJoinResponse from(MeetingJoinResult result, String wsUrl) {
		Meeting meeting = result.meeting();
		MeetingParticipant participant = result.participant();
		return new MeetingJoinResponse(
				meeting.getId(),
				meeting.getTitle(),
				meeting.getStatus().name(),
				participant.getRole().name(),
				participant.getJoinedAt(),
				meeting.getStartedAt(),
				wsUrl
		);
	}
}
