package com.synq.backend.domain.ai.rag;

/**
 * 참고자료 업로드가 끝난 뒤 호출한다. 청킹·임베딩·저장을 백그라운드에서 수행한다.
 *
 * projectId 는 검색 스코프다. ai/rag 가 project 도메인을 조회하지 않도록
 * 호출자가 넘긴다.
 */
public interface DocumentIndexer {

	void indexAsync(Long referenceMaterialId, Long projectId, String extractedText);

	/**
	 * 참고자료가 삭제되면 청크도 지운다.
	 *
	 * <p>참고자료는 소프트 삭제라 FK 의 ON DELETE CASCADE 가 동작하지 않는다. 지우지 않으면
	 * 사용자가 삭제한 문서의 내용이 계속 검색되어 3-hint 와 AI Chat 답변에 실려 나간다.
	 */
	void deleteIndex(Long referenceMaterialId);
}
