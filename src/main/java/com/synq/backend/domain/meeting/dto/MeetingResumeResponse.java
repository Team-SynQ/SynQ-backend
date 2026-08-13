package com.synq.backend.domain.meeting.dto;

import com.synq.backend.domain.meeting.entity.Meeting;

public record MeetingResumeResponse(
		Long meetingId,
		String status,
		boolean paused,
		long activeSeconds
) {
	public static MeetingResumeResponse from(Meeting meeting) {
		return new MeetingResumeResponse(
				meeting.getId(),
				meeting.getStatus().name(),
				meeting.isPaused(),
				meeting.activeSeconds()
		);
	}
}
