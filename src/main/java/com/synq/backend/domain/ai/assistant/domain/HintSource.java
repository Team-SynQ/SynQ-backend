package com.synq.backend.domain.ai.assistant.domain;

/**
 * 3-hint 생성 경로. 수동 요청과 중요 발화 기반 자동 생성 결과를 구분한다.
 */
public enum HintSource {
	MANUAL,
	AUTO
}
