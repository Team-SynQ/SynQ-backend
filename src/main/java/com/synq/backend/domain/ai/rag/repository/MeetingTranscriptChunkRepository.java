package com.synq.backend.domain.ai.rag.repository;

import com.synq.backend.domain.ai.rag.entity.MeetingTranscriptChunk;
import com.synq.backend.domain.ai.rag.search.ChunkSearchRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MeetingTranscriptChunkRepository extends JpaRepository<MeetingTranscriptChunk, Long> {

	List<MeetingTranscriptChunk> findByMeetingIdOrderByChunkIndexAsc(Long meetingId);

	// 재인덱싱 시 기존 청크를 전부 지우고 다시 만든다(all-or-nothing).
	void deleteByMeetingId(Long meetingId);

	/**
	 * 프로젝트 안의 과거 회의 전사 청크를 유사도 내림차순으로 찾는다.
	 * 회의 단위가 아니라 프로젝트 단위인 이유는, 예상 질문 추천이 "이 프로젝트의 이전 회의들" 을
	 * 검색하기 때문이다.
	 *
	 * 쿼리 구조는 {@link DocumentChunkRepository#searchByProject} 와 같다.
	 * ORDER BY 를 유사도가 아니라 거리 오름차순으로 두어야 HNSW 인덱스를 탄다.
	 * 별칭의 큰따옴표는 Postgres 가 식별자를 소문자로 접는 것을 막는다.
	 */
	@Query(value = """
			SELECT id          AS "chunkId",
			       meeting_id  AS "sourceId",
			       chunk_index AS "chunkIndex",
			       content     AS "content",
			       1 - (embedding <=> CAST(:embedding AS vector)) AS "similarity"
			FROM meeting_transcript_chunk
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
