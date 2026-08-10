package com.synq.backend.domain.ai.assistant.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ai.assistant.hint.* 바인딩. RAG 검색 검증용 기본값(ai.rag.search.*)과 별도로,
 * 3-hint 는 속도 우선이라 더 타이트하게 튜닝한다.
 * BackendApplication 의 @ConfigurationPropertiesScan 이 등록한다.
 *
 * @param searchWindow 검색 질의에만 쓰는 범위. 프롬프트에 넣는 windowBefore/windowAfter 와 별개다.
 *                     조회 범위 밖의 값을 넣으면 해당 세그먼트가 없어 조용히 무시된다.
 * @param focusRepeat  검색 질의에서 클릭한 발화를 반복하는 횟수. 반복이 곧 가중이다.
 *                     1 이면 반복 없음 = 가중 없음.
 */
@ConfigurationProperties(prefix = "ai.assistant.hint")
public record AssistantHintProperties(
		int windowBefore,
		int windowAfter,
		int searchWindow,
		int focusRepeat,
		int topK,
		double minSimilarity
) {

	public AssistantHintProperties {
		if (windowBefore < 0 || windowAfter < 0) {
			throw new IllegalArgumentException("윈도우 크기는 0 이상이어야 합니다.");
		}
		if (searchWindow < 0) {
			throw new IllegalArgumentException("검색 질의 윈도우는 0 이상이어야 합니다: " + searchWindow);
		}
		// 0 이면 검색 질의에서 클릭 발화가 통째로 빠져 질의가 비고, ChunkSearchQuery 가 이를 거부한다.
		if (focusRepeat < 1) {
			throw new IllegalArgumentException("focus-repeat 은 1 이상이어야 합니다: " + focusRepeat);
		}
		if (topK <= 0) {
			throw new IllegalArgumentException("topK 는 1 이상이어야 합니다: " + topK);
		}
	}
}
