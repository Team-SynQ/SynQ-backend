package com.synq.backend.domain.ai.summary.domain;

import java.time.Instant;
import java.util.UUID;

public record MeetingSummary(
		Long meetingId,
		UUID jobId,
		int version,
		GeneratedSummary content,
		Instant generatedAt
) {
	public static MeetingSummary from(Long meetingId, UUID jobId, GeneratedSummary content) {
		return new MeetingSummary(meetingId, jobId, 0, content, Instant.now());
	}

	public MeetingSummary withVersion(int assignedVersion) {
		return new MeetingSummary(meetingId, jobId, assignedVersion, content, generatedAt);
	}
}
