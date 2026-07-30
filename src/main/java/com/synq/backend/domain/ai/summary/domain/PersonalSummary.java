package com.synq.backend.domain.ai.summary.domain;

import java.time.Instant;
import java.util.UUID;

public record PersonalSummary(
		Long meetingId,
		UUID jobId,
		Long userId,
		String role,
		int version,
		GeneratedPersonalSummary content,
		Instant generatedAt
) {
	public static PersonalSummary from(
			Long meetingId,
			UUID jobId,
			PersonalSummaryTarget target,
			int version,
			GeneratedPersonalSummary content
	) {
		return new PersonalSummary(
				meetingId,
				jobId,
				target.userId(),
				target.roleDescription(),
				version,
				content,
				Instant.now()
		);
	}
}
