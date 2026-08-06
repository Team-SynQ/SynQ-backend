package com.synq.backend.domain.ai.summary.api.dto;

import com.synq.backend.domain.ai.summary.domain.SummaryJob;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record SummaryJobResponse(
		UUID jobId,
		String status,
		@Schema(description = "개인 요약 생성에 최종 실패한 건수", example = "1")
		int failedPersonalSummaryCount,
		String modelName,
		String promptVersion,
		String errorMessage,
		Instant completedAt
) {
	public static SummaryJobResponse from(SummaryJob job) {
		return new SummaryJobResponse(
				job.id(),
				job.status().name(),
				job.failedPersonalSummaryCount(),
				job.modelName(),
				job.promptVersion(),
				job.errorMessage(),
				job.completedAt()
		);
	}
}
