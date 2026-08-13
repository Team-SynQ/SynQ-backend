package com.synq.backend.domain.meeting.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MeetingListResponse(
		Long meetingId,
		String title,
		String status,
		LocalDateTime createdAt,
		Long durationSeconds,
		Host host,
		List<String> keyTopics,
		boolean paused,
		long activeSeconds
) {
	public record Host(
			Long userId,
			String name,
			String profileImageUrl
	) {
	}
}
