package com.synq.backend.domain.ai.rag.search;

/**
 * 네이티브 벡터 검색 쿼리의 결과 한 행.
 * Spring Data 인터페이스 projection 이므로 게터 이름이 SQL 별칭과 일치해야 한다.
 */
public interface ChunkSearchRow {

	Long getChunkId();

	Long getReferenceMaterialId();

	Integer getChunkIndex();

	String getContent();

	Double getSimilarity();
}
