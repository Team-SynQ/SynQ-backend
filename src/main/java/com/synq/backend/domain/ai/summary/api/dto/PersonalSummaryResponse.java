package com.synq.backend.domain.ai.summary.api.dto;

import com.synq.backend.domain.ai.summary.domain.PersonalSummary;
import java.time.Instant;
import java.util.List;

public record PersonalSummaryResponse(
		Long meetingId,
		Long userId,
		int version,
		String role,
		String personalSummary,
		List<String> keyPoints,
		List<String> myActionItems,
		List<String> followUpQuestions,
		Instant generatedAt
) {
	public static PersonalSummaryResponse from(PersonalSummary summary) {
		var content = summary.content();
		return new PersonalSummaryResponse(
				summary.meetingId(),
				summary.userId(),
				summary.version(),
				summary.role(),
				content.personalSummary(),
				content.keyPoints(),
				content.myActionItems(),
				content.followUpQuestions(),
				summary.generatedAt()
		);
	}
}
