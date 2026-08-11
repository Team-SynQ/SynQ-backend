package com.synq.backend.domain.ai.event;

/**
 * 자동 3-hint 생성 후 대상 사용자에게만 보내는 SSE 데이터 형식이다.
 */
public record AutoHintCreatedPayload(
		Long meetingId,
		Long segmentId,
		String meaning,
		String myImpact,
		String teamQuestion,
		int importance,
		String triggerReason
) {
}
