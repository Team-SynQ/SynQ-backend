package com.synq.backend.domain.transcript.dto;

import com.synq.backend.domain.transcript.entity.MeetingRecordingSegment;

import java.time.LocalDateTime;

public record MeetingRecordingSegmentResponse(
		Long segmentId,
		String url,
		LocalDateTime createdAt
) {
	public static MeetingRecordingSegmentResponse of(MeetingRecordingSegment segment, String url) {
		return new MeetingRecordingSegmentResponse(segment.getId(), url, segment.getCreatedAt());
	}
}
