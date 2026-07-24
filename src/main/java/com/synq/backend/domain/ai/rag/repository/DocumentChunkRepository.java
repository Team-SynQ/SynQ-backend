package com.synq.backend.domain.ai.rag.repository;

import com.synq.backend.domain.ai.rag.entity.DocumentChunk;
import com.synq.backend.domain.ai.rag.search.ChunkSearchRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

	List<DocumentChunk> findByReferenceMaterialIdOrderByChunkIndexAsc(Long referenceMaterialId);

	// 재처리 시 기존 청크를 전부 지우고 다시 만든다(all-or-nothing).
	void deleteByReferenceMaterialId(Long referenceMaterialId);

	/**
	 * 프로젝트 안에서 질의 벡터와 가까운 청크를 유사도 내림차순으로 찾는다.
	 *
	 * ORDER BY 를 유사도(1 - 거리)가 아니라 거리 오름차순으로 두는 이유는,
	 * HNSW 인덱스가 {@code <=>} 표현식 기준이라 유사도로 정렬하면 인덱스를 타지 못하기 때문이다.
	 *
	 * 별칭에 큰따옴표를 씌우는 이유는, Postgres 가 따옴표 없는 식별자를 소문자로 접기 때문이다.
	 * 접히면 projection 게터 이름과 어긋난다.
	 *
	 * @param embedding {@link com.synq.backend.domain.ai.rag.search.VectorLiteral} 로 만든 문자열
	 * @param minSimilarity 이 값 미만은 제외한다. 코사인 유사도의 하한인 -1 을 넘기면 필터가 없는 것과 같다.
	 */
	@Query(value = """
			SELECT id                    AS "chunkId",
			       reference_material_id AS "referenceMaterialId",
			       chunk_index           AS "chunkIndex",
			       content               AS "content",
			       1 - (embedding <=> CAST(:embedding AS vector)) AS "similarity"
			FROM document_chunk
			WHERE project_id = :projectId
			  AND 1 - (embedding <=> CAST(:embedding AS vector)) >= :minSimilarity
			ORDER BY embedding <=> CAST(:embedding AS vector)
			LIMIT :topK
			""", nativeQuery = true)
	List<ChunkSearchRow> searchByProject(
			@Param("projectId") Long projectId,
			@Param("embedding") String embedding,
			@Param("minSimilarity") double minSimilarity,
			@Param("topK") int topK);
}
