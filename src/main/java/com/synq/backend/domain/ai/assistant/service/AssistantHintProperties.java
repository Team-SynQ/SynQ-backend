package com.synq.backend.domain.ai.assistant.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ai.assistant.hint.* 바인딩. RAG 검색 검증용 기본값(ai.rag.search.*)과 별도로,
 * 3-hint 는 속도 우선이라 더 타이트하게 튜닝한다.
 * BackendApplication 의 @ConfigurationPropertiesScan 이 등록한다.
 */
@ConfigurationProperties(prefix = "ai.assistant.hint")
public record AssistantHintProperties(
		int windowBefore,
		int windowAfter,
		int topK,
		double minSimilarity
) {

	public AssistantHintProperties {
		if (windowBefore < 0 || windowAfter < 0) {
			throw new IllegalArgumentException("윈도우 크기는 0 이상이어야 합니다.");
		}
		if (topK <= 0) {
			throw new IllegalArgumentException("topK 는 1 이상이어야 합니다: " + topK);
		}
	}
}
