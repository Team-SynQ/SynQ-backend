package com.synq.backend.domain.ai.assistant.domain;

/**
 * 3-hint 를 생성하는 AI 제공자 포트. openai / fake 구현을 프로퍼티로 고른다.
 */
public interface HintAiClient {

	HintResult generate(HintInput input);
}
