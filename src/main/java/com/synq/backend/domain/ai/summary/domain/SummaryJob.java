package com.synq.backend.domain.ai.summary.domain;

import java.time.Instant;
import java.time.Duration;
import java.util.UUID;

public record SummaryJob(
		UUID id,
		Long meetingId,
		SummaryJobStatus status,
		int retryCount,
		String modelName,
		String promptVersion,
		String errorMessage,
		Instant createdAt,
		Instant startedAt,
		Instant completedAt
) {

	/** 요약 생성 요청 직후의 상태. 실제 처리는 SummaryJobProcessor가 시작한다. */
	public static SummaryJob queued(Long meetingId) {
		return queued(meetingId, null, "v1");
	}

	public static SummaryJob queued(Long meetingId, String modelName, String promptVersion) {
		return new SummaryJob(UUID.randomUUID(), meetingId, SummaryJobStatus.QUEUED,
				0, modelName, promptVersion, null, Instant.now(), null, null);
	}

	public SummaryJob start() {
		return new SummaryJob(id, meetingId, SummaryJobStatus.PROCESSING,
				retryCount, modelName, promptVersion, null, createdAt, Instant.now(), null);
	}

	public SummaryJob complete() {
		return new SummaryJob(id, meetingId, SummaryJobStatus.COMPLETED,
				retryCount, modelName, promptVersion, null, createdAt, startedAt, Instant.now());
	}

	public SummaryJob fail(String message) {
		return new SummaryJob(id, meetingId, SummaryJobStatus.FAILED,
					retryCount, modelName, promptVersion, message, createdAt, startedAt, Instant.now());
	}

	public boolean isStale(Instant now, Duration timeout) {
		Instant activeSince = startedAt == null ? createdAt : startedAt;
		return status != SummaryJobStatus.COMPLETED
				&& status != SummaryJobStatus.FAILED
				&& activeSince.plus(timeout).isBefore(now);
	}
}
