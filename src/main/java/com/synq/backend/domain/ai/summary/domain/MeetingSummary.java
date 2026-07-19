package com.synq.backend.domain.ai.summary.domain;

import java.time.Instant;

public record MeetingSummary(
		Long meetingId,
		GeneratedSummary content,
		Instant generatedAt
) {
	public static MeetingSummary from(Long meetingId, GeneratedSummary content) {
		return new MeetingSummary(meetingId, content, Instant.now());
	}
}
