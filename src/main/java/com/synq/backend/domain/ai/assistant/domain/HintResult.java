package com.synq.backend.domain.ai.assistant.domain;

/**
 * 3-hint 결과. meaning(의미) / myImpact(내 영향) / teamQuestion(팀 질문).
 */
public record HintResult(String meaning, String myImpact, String teamQuestion) {
}
