package com.synq.backend.domain.ai.assistant.application;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 중요 전사 기반 자동 3-hint 생성 기준이다. 회의별 생성 개수는 제한하지 않는다.
 */
@Validated
@ConfigurationProperties(prefix = "ai.assistant.auto-hint")
public record AutoHintProperties(
		boolean enabled,
		@Min(0) @Max(100) int importanceThreshold
) {
}
