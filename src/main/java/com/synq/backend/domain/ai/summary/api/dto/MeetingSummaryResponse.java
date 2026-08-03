package com.synq.backend.domain.ai.summary.api.dto;

import com.synq.backend.domain.ai.summary.domain.MeetingSummary;
import com.synq.backend.domain.ai.summary.domain.DiscussionSection;
import java.time.Instant;
import java.util.List;

public record MeetingSummaryResponse(
		Long meetingId,
		int version,
		String oneLineSummary,
		List<String> keyTopics,
		List<DiscussionSection> discussionSections,
		List<String> decisions,
		List<String> tentativeDirections,
		List<String> confirmationItems,
		Instant generatedAt
) {
	public static MeetingSummaryResponse from(MeetingSummary summary) {
		var content = summary.content();
		return new MeetingSummaryResponse(
				summary.meetingId(), summary.version(), content.oneLineSummary(), content.keyTopics(),
				content.discussionSections(), content.decisions(), content.tentativeDirections(),
				content.confirmationItems(), summary.generatedAt());
	}
}
