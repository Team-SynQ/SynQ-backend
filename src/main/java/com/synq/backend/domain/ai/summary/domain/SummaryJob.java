package com.synq.backend.domain.ai.summary.domain;

import java.time.Instant;
import java.time.Duration;
import java.util.UUID;

public record SummaryJob(
		UUID id,
		Long meetingId,
		SummaryJobStatus status,
		int retryCount,
		int failedPersonalSummaryCount,
		String modelName,
		String promptVersion,
		String errorMessage,
		Instant createdAt,
		Instant startedAt,
		Instant completedAt
) {
	public SummaryJob {
		if (failedPersonalSummaryCount < 0) {
			throw new IllegalArgumentException("개인 요약 실패 건수는 음수일 수 없습니다.");
		}
	}

	/** 요약 생성 요청 직후의 상태. 실제 처리는 SummaryJobProcessor가 시작한다. */
	public static SummaryJob queued(Long meetingId) {
		return queued(meetingId, null, "v1");
	}

	public static SummaryJob queued(Long meetingId, String modelName, String promptVersion) {
		return new SummaryJob(UUID.randomUUID(), meetingId, SummaryJobStatus.QUEUED,
				0, 0, modelName, promptVersion, null, Instant.now(), null, null);
	}

	public SummaryJob start() {
		if (status != SummaryJobStatus.QUEUED) {
			throw new IllegalStateException("대기 중인 요약 작업만 시작할 수 있습니다.");
		}
		return new SummaryJob(id, meetingId, SummaryJobStatus.PROCESSING,
				retryCount, 0, modelName, promptVersion, null, createdAt, Instant.now(), null);
	}

	public SummaryJob complete(int failedPersonalSummaryCount) {
		if (status != SummaryJobStatus.PROCESSING) {
			throw new IllegalStateException("실행 중인 요약 작업만 완료할 수 있습니다.");
		}
		SummaryJobStatus completedStatus = failedPersonalSummaryCount == 0
				? SummaryJobStatus.COMPLETED
				: SummaryJobStatus.COMPLETED_WITH_ERRORS;
		return new SummaryJob(id, meetingId, completedStatus,
				retryCount, failedPersonalSummaryCount, modelName, promptVersion, null,
				createdAt, startedAt, Instant.now());
	}

	public SummaryJob complete() {
		return complete(0);
	}

	public SummaryJob fail(String message) {
		if (status != SummaryJobStatus.QUEUED && status != SummaryJobStatus.PROCESSING) {
			throw new IllegalStateException("종료된 요약 작업은 실패 처리할 수 없습니다.");
		}
		return new SummaryJob(id, meetingId, SummaryJobStatus.FAILED,
				retryCount, failedPersonalSummaryCount, modelName, promptVersion, message,
				createdAt, startedAt, Instant.now());
	}

	public boolean isStale(Instant now, Duration timeout) {
		Instant activeSince = startedAt == null ? createdAt : startedAt;
		return status != SummaryJobStatus.COMPLETED
				&& status != SummaryJobStatus.COMPLETED_WITH_ERRORS
				&& status != SummaryJobStatus.FAILED
				&& activeSince.plus(timeout).isBefore(now);
	}
}
