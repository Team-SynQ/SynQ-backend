package com.synq.backend.domain.meeting.dto;

import com.synq.backend.domain.meeting.entity.Meeting;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public record MeetingEndResponse(
		Long meetingId,
		String status,
		LocalDateTime endedAt,
		@Schema(description = "회의 종료와 함께 접수된 AI 요약 작업 ID. 작업 상태 조회 API의 jobId로 사용한다")
		UUID summaryJobId
) {
	public static MeetingEndResponse from(Meeting meeting, UUID summaryJobId) {
		return new MeetingEndResponse(
				meeting.getId(),
				meeting.getStatus().name(),
				meeting.getEndedAt(),
				summaryJobId
		);
	}
}
