package com.synq.backend.domain.ai.summary.domain;

public enum SummaryJobStatus {
	QUEUED,
	PROCESSING,
	COMPLETED,
	COMPLETED_WITH_ERRORS,
	FAILED
}
