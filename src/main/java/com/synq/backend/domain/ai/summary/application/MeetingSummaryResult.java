package com.synq.backend.domain.ai.summary.application;

import com.synq.backend.domain.ai.summary.domain.MeetingSummary;

/**
 * 저장된 요약과 회의 도메인의 제목을 함께 담은 조회 결과.
 */
public record MeetingSummaryResult(
		String title,
		MeetingSummary summary
) {
}
