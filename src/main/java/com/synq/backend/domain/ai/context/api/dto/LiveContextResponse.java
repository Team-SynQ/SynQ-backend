package com.synq.backend.domain.ai.context.api.dto;

import com.synq.backend.domain.ai.context.domain.LiveContext;
import java.util.List;

public record LiveContextResponse(
		Long meetingId,
		String rollingSummary,
		String currentTopic,
		List<String> decisions,
		List<String> actionItems,
		List<String> openQuestions,
		Long lastSegmentId,
		Integer lastSequenceIndex
) {

	public static LiveContextResponse from(LiveContext context) {
		return new LiveContextResponse(
				context.getMeetingId(),
				context.getRollingSummary(),
				context.getCurrentTopic(),
				context.getDecisions(),
				context.getActionItems(),
				context.getOpenQuestions(),
				context.getLastSegmentId(),
				context.getLastSequenceIndex()
		);
	}
}
