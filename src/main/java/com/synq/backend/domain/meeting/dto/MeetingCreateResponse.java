package com.synq.backend.domain.meeting.dto;

import com.synq.backend.domain.meeting.entity.Meeting;

import java.time.LocalDateTime;

public record MeetingCreateResponse(
		Long meetingId,
		String title,
		String status,
		LocalDateTime startedAt,
		String wsUrl
) {
	public static MeetingCreateResponse from(Meeting meeting, String wsUrl) {
		return new MeetingCreateResponse(
				meeting.getId(),
				meeting.getTitle(),
				meeting.getStatus().name(),
				meeting.getStartedAt(),
				wsUrl
		);
	}
}
