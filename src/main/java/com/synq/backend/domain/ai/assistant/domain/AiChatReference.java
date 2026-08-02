package com.synq.backend.domain.ai.assistant.domain;

/**
 * AI Chat 답변에 활용할 프로젝트 참고자료 청크다.
 */
public record AiChatReference(Long referenceMaterialId, Long chunkId, String content) {
}
