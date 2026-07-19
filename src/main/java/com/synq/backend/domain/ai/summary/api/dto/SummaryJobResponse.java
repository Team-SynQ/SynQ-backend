package com.synq.backend.domain.ai.summary.api.dto;

import com.synq.backend.domain.ai.summary.domain.SummaryJob;
import java.time.Instant;
import java.util.UUID;

public record SummaryJobResponse(UUID jobId, String status, String errorMessage, Instant completedAt) {
	public static SummaryJobResponse from(SummaryJob job) {
		return new SummaryJobResponse(job.id(), job.status().name(), job.errorMessage(), job.completedAt());
	}
}
