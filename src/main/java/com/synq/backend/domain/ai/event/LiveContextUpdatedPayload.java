package com.synq.backend.domain.ai.event;

import java.util.List;

/**
 * Live Context 갱신 시 프론트엔드에 보내는 SSE 데이터 형식이다.
 */
public record LiveContextUpdatedPayload(
		Long meetingId,
		String rollingSummary,
		String currentTopic,
		List<String> decisions,
		List<String> actionItems,
		List<String> openQuestions,
		Long lastSegmentId,
		Integer lastSequenceIndex
) {
}
