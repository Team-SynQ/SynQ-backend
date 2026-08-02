package com.synq.backend.domain.ai.assistant.domain;

/**
 * Chat 프롬프트에 포함할 전사 발화다.
 */
public record AiChatTranscript(Long id, String speakerLabel, String content) {
}
