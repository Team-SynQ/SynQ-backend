package com.synq.backend.domain.ai.summary.domain;

import java.time.Instant;
import java.util.UUID;

public record SummaryJob(
		UUID id,
		Long meetingId,
		SummaryJobStatus status,
		String errorMessage,
		Instant createdAt,
		Instant startedAt,
		Instant completedAt
) {

	/** 요약 생성 요청 직후의 상태. 실제 처리는 SummaryJobProcessor가 시작한다. */
	public static SummaryJob queued(Long meetingId) {
		return new SummaryJob(UUID.randomUUID(), meetingId, SummaryJobStatus.QUEUED,
				null, Instant.now(), null, null);
	}

	public SummaryJob start() {
		return new SummaryJob(id, meetingId, SummaryJobStatus.PROCESSING,
				null, createdAt, Instant.now(), null);
	}

	public SummaryJob complete() {
		return new SummaryJob(id, meetingId, SummaryJobStatus.COMPLETED,
				null, createdAt, startedAt, Instant.now());
	}

	public SummaryJob fail(String message) {
		return new SummaryJob(id, meetingId, SummaryJobStatus.FAILED,
				message, createdAt, startedAt, Instant.now());
	}
}
