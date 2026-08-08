package com.synq.backend.domain.ai.summary.application;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 회의 후 요약에서 사용할 RAG 검색 범위.
 *
 * <p>요약은 전체 전사를 이미 직접 전달하므로, RAG는 참고자료와 이전 회의만 보강하는 용도다.</p>
 */
@Validated
@ConfigurationProperties(prefix = "ai.summary.rag")
public record SummaryRagProperties(
		@Positive int topK,
		double minSimilarity,
		@Min(100) int maxQueryChars
) {
	@AssertTrue(message = "요약 RAG minSimilarity는 -1 이상 1 이하의 유한한 값이어야 합니다.")
	public boolean hasValidMinSimilarity() {
		return Double.isFinite(minSimilarity) && minSimilarity >= -1.0 && minSimilarity <= 1.0;
	}
}
