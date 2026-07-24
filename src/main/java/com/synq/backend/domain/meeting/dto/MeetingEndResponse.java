package com.synq.backend.domain.meeting.dto;

import com.synq.backend.domain.meeting.entity.Meeting;

import java.time.LocalDateTime;

public record MeetingEndResponse(
		Long meetingId,
		String status,
		LocalDateTime endedAt
) {
	public static MeetingEndResponse from(Meeting meeting) {
		return new MeetingEndResponse(
				meeting.getId(),
				meeting.getStatus().name(),
				meeting.getEndedAt()
		);
	}
}
