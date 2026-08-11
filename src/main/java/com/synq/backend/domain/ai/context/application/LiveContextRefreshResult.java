package com.synq.backend.domain.ai.context.application;

import com.synq.backend.domain.ai.context.domain.AutoHintDecision;
import com.synq.backend.domain.ai.context.domain.LiveContext;

/**
 * Live Context 저장 결과와 이번 확정 전사에만 해당하는 자동 힌트 판단을 함께 전달한다.
 */
public record LiveContextRefreshResult(
		LiveContext context,
		AutoHintDecision autoHintDecision,
		boolean hasMorePending
) {
	public LiveContextRefreshResult(LiveContext context, AutoHintDecision autoHintDecision) {
		this(context, autoHintDecision, false);
	}
}
