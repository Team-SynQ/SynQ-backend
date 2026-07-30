package com.synq.backend.domain.ai.summary.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.AssertTrue;
import java.time.Duration;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ai.summary")
public record SummaryProperties(
		@NotBlank String modelName,
		@NotBlank String promptVersion,
		@Positive int maxInputChars,
		@NotNull @DefaultValue("30m") Duration activeJobTimeout
) {
	@ConstructorBinding
	public SummaryProperties {
	}

	public SummaryProperties(String modelName, String promptVersion, int maxInputChars) {
		this(modelName, promptVersion, maxInputChars, Duration.ofMinutes(30));
	}

	@AssertTrue(message = "활성 요약 Job timeout은 양수여야 합니다.")
	public boolean hasValidActiveJobTimeout() {
		return activeJobTimeout != null && !activeJobTimeout.isZero() && !activeJobTimeout.isNegative();
	}
}
