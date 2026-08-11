package com.synq.backend.domain.meeting.dto;

import java.time.LocalDateTime;

public record MeetingParticipantResponse(
		Long userId,
		String name,
		String profileImageUrl,
		String role,
		LocalDateTime joinedAt
) {
}
