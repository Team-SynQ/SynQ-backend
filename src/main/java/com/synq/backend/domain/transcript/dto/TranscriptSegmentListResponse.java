package com.synq.backend.domain.transcript.dto;

import com.synq.backend.domain.transcript.entity.TranscriptSegment;

import java.util.List;

public record TranscriptSegmentListResponse(
		Long meetingId,
		List<TranscriptSegmentResponse> segments
) {
	public static TranscriptSegmentListResponse from(Long meetingId, List<TranscriptSegment> segments) {
		return new TranscriptSegmentListResponse(
				meetingId,
				segments.stream().map(TranscriptSegmentResponse::from).toList()
		);
	}
}
