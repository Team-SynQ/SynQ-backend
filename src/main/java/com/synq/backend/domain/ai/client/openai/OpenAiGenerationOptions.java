package com.synq.backend.domain.ai.client.openai;

import org.springframework.util.StringUtils;

/** 기능별 OpenAI 생성 요청에 적용할 모델 옵션이다. */
public record OpenAiGenerationOptions(
		String model,
		String reasoningEffort,
		int maxOutputTokens
) {
	public static final String REASONING_EFFORT_PATTERN = "none|minimal|low|medium|high|xhigh|max";

	public OpenAiGenerationOptions {
		if (!StringUtils.hasText(model)) {
			throw new IllegalArgumentException("OpenAI 모델은 비어 있을 수 없습니다.");
		}
		if (!StringUtils.hasText(reasoningEffort)) {
			throw new IllegalArgumentException("OpenAI 추론 강도는 비어 있을 수 없습니다.");
		}
		if (!reasoningEffort.matches(REASONING_EFFORT_PATTERN)) {
			throw new IllegalArgumentException("지원하지 않는 OpenAI 추론 강도입니다.");
		}
		if (maxOutputTokens <= 0) {
			throw new IllegalArgumentException("OpenAI 최대 출력 토큰은 양수여야 합니다.");
		}
	}
}
