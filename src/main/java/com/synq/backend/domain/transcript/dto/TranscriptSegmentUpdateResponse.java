package com.synq.backend.domain.transcript.dto;

import com.synq.backend.domain.transcript.entity.TranscriptSegment;

import java.time.LocalDateTime;

public record TranscriptSegmentUpdateResponse(
		Long segmentId,
		Long meetingId,
		String content,
		boolean isModified,
		LocalDateTime updatedAt
) {
	public static TranscriptSegmentUpdateResponse from(TranscriptSegment segment) {
		return new TranscriptSegmentUpdateResponse(
				segment.getId(),
				segment.getMeetingId(),
				segment.getContent(),
				segment.isModified(),
				segment.getUpdatedAt()
		);
	}
}
