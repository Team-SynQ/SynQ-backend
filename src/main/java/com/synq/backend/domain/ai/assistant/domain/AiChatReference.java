package com.synq.backend.domain.ai.assistant.domain;

import com.synq.backend.domain.ai.rag.search.ChunkSource;

/**
 * AI Chat 답변에 활용할 청크다. 프로젝트 참고자료와 과거 회의 전사 둘 다 들어온다.
 */
public record AiChatReference(ChunkSource source, Long sourceId, Long chunkId, String content) {
}
