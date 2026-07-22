package com.synq.backend.domain.ai.rag;

/**
 * 참고자료 업로드가 끝난 뒤 호출한다. 청킹·임베딩·저장을 백그라운드에서 수행한다.
 *
 * projectId 는 검색 스코프다. ai/rag 가 project 도메인을 조회하지 않도록
 * 호출자가 넘긴다.
 */
public interface DocumentIndexer {

	void indexAsync(Long referenceMaterialId, Long projectId, String extractedText);
}
