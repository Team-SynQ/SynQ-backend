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
		String wsUrl
) {
	public static MeetingJoinResponse from(MeetingJoinResult result) {
		Meeting meeting = result.meeting();
		MeetingParticipant participant = result.participant();
		return new MeetingJoinResponse(
				meeting.getId(),
				meeting.getTitle(),
				meeting.getStatus().name(),
				participant.getRole().name(),
				participant.getJoinedAt(),
				// TODO: 실제 STT WebSocket 엔드포인트가 정해지면 그 경로로 교체한다.
				"/ws/meetings/%d/stt".formatted(meeting.getId())
		);
	}
}
