package com.synq.backend.domain.ai.assistant.domain;

/**
 * 후속 질문의 의미를 보존하기 위해 전달하는 이전 대화 한 단위다.
 */
public record AiChatTurn(String question, String answer) {
}
