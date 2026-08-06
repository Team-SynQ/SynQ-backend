package com.synq.backend.domain.transcript.dto;

import com.synq.backend.domain.transcript.entity.TranscriptSegment;

public record TranscriptSegmentResponse(
		Long segmentId,
		Integer sequenceIndex,
		Integer startMs,
		Integer endMs,
		String content,
		String speakerLabel,
		boolean isModified
) {
	public static TranscriptSegmentResponse from(TranscriptSegment segment) {
		return new TranscriptSegmentResponse(
				segment.getId(),
				segment.getSequenceIndex(),
				segment.getStartMs(),
				segment.getEndMs(),
				segment.getContent(),
				segment.getSpeakerLabel(),
				segment.isModified()
		);
	}
}
