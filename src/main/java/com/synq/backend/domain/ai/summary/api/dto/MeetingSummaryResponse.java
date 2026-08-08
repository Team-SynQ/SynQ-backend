package com.synq.backend.domain.ai.summary.api.dto;

import com.synq.backend.domain.ai.summary.application.MeetingSummaryResult;
import com.synq.backend.domain.ai.summary.domain.DiscussionSection;
import com.synq.backend.domain.ai.summary.domain.MeetingSummary;
import java.time.Instant;
import java.util.List;

public record MeetingSummaryResponse(
		Long meetingId,
		String title,
		int version,
		String oneLineSummary,
		List<String> keyTopics,
		List<DiscussionSection> discussionSections,
		List<String> decisions,
		List<String> tentativeDirections,
		List<String> confirmationItems,
		Instant generatedAt
) {
	public static MeetingSummaryResponse from(MeetingSummaryResult result) {
		MeetingSummary summary = result.summary();
		var content = summary.content();
		return new MeetingSummaryResponse(
				summary.meetingId(), result.title(), summary.version(), content.oneLineSummary(), content.keyTopics(),
				content.discussionSections(), content.decisions(), content.tentativeDirections(),
				content.confirmationItems(), summary.generatedAt());
	}
}
