package com.synq.backend.domain.ai.summary.api.dto;

import com.synq.backend.domain.ai.summary.domain.SummaryJob;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record SummaryJobResponse(
		@Schema(description = "요약 생성 작업 ID")
		UUID jobId,
		@Schema(
				description = "작업 상태. COMPLETED_WITH_ERRORS는 전체 요약은 성공했지만 일부 개인 요약이 실패한 부분 성공을 의미합니다.",
				allowableValues = {"QUEUED", "PROCESSING", "COMPLETED", "COMPLETED_WITH_ERRORS", "FAILED"},
				example = "PROCESSING"
		)
		String status,
		@Schema(description = "재시도 후에도 생성하지 못한 개인 요약 건수. COMPLETED_WITH_ERRORS일 때 1 이상입니다.", example = "1")
		int failedPersonalSummaryCount,
		@Schema(description = "요약 생성에 사용한 AI 모델명")
		String modelName,
		@Schema(description = "요약 프롬프트 버전")
		String promptVersion,
		@Schema(description = "작업 실패 사유. FAILED가 아니면 null일 수 있습니다.", nullable = true)
		String errorMessage,
		@Schema(description = "작업 종료 시각. QUEUED 또는 PROCESSING 상태에서는 null입니다.", nullable = true)
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
