package com.synq.backend.domain.meeting.dto;

import com.synq.backend.domain.meeting.entity.Meeting;

public record MeetingPauseResponse(
		Long meetingId,
		String status,
		boolean paused,
		long activeSeconds
) {
	public static MeetingPauseResponse from(Meeting meeting) {
		return new MeetingPauseResponse(
				meeting.getId(),
				meeting.getStatus().name(),
				meeting.isPaused(),
				meeting.activeSeconds()
		);
	}
}
