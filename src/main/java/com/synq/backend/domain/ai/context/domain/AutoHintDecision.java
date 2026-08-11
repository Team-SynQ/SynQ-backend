package com.synq.backend.domain.ai.context.domain;

/**
 * 새 확정 전사가 참여자별 자동 3-hint 생성 대상인지에 대한 AI 판단이다.
 */
public record AutoHintDecision(
		boolean shouldGenerate,
		Long targetSegmentId,
		int importance,
		String triggerReason
) {

	public AutoHintDecision {
		importance = Math.max(0, Math.min(100, importance));
		triggerReason = triggerReason == null ? "" : triggerReason;
	}

	public static AutoHintDecision none() {
		return new AutoHintDecision(false, null, 0, "");
	}
}
