package com.synq.backend.domain.ai.summary.api.dto;

import com.synq.backend.domain.ai.summary.domain.MeetingSummary;
import java.time.Instant;
import java.util.List;

public record MeetingSummaryResponse(
		Long meetingId,
		String overallSummary,
		List<String> keyTopics,
		List<String> decisions,
		List<String> actionItems,
		List<String> openQuestions,
		Instant generatedAt
) {
	public static MeetingSummaryResponse from(MeetingSummary summary) {
		var content = summary.content();
		return new MeetingSummaryResponse(
				summary.meetingId(), content.overallSummary(), content.keyTopics(), content.decisions(),
				content.actionItems(), content.openQuestions(), summary.generatedAt());
	}
}
