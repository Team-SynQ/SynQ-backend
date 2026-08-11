package com.synq.backend.domain.ai.context.application;

import jakarta.validation.constraints.Min;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Live Context 갱신 배치 기준이다. */
@Validated
@ConfigurationProperties(prefix = "ai.live-context.batch")
public record LiveContextBatchProperties(
		@Min(1) int segmentCount,
		@Min(1) int maxSegmentsPerRequest,
		Duration maxDelay,
		Duration recoveryDelay
) {

	public LiveContextBatchProperties {
		if (maxDelay == null || maxDelay.isNegative() || maxDelay.isZero()) {
			throw new IllegalArgumentException("Live Context 최대 대기 시간은 0보다 커야 합니다.");
		}
		if (recoveryDelay == null || recoveryDelay.isNegative() || recoveryDelay.isZero()) {
			throw new IllegalArgumentException("Live Context 복구 주기는 0보다 커야 합니다.");
		}
	}
}
