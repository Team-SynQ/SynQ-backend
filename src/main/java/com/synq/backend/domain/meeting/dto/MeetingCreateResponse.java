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
	public static MeetingCreateResponse from(Meeting meeting) {
		return new MeetingCreateResponse(
				meeting.getId(),
				meeting.getTitle(),
				meeting.getStatus().name(),
				meeting.getStartedAt(),
				// TODO: 실제 STT WebSocket 엔드포인트가 정해지면 그 경로로 교체한다.
				"/ws/meetings/%d/stt".formatted(meeting.getId())
		);
	}
}
