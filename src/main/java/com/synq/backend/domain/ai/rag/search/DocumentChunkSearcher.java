package com.synq.backend.domain.ai.rag.search;

import com.synq.backend.domain.ai.client.EmbeddingClient;
import com.synq.backend.domain.ai.rag.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 참고자료 청크(document_chunk)를 대상으로 검색한다.
 * 회의 전사 청크 검색은 별도 구현체로 추가한다.
 */
@Component
@RequiredArgsConstructor
public class DocumentChunkSearcher implements ChunkSearcher {

	private final EmbeddingClient embeddingClient;
	private final DocumentChunkRepository repository;

	@Override
	@Transactional(readOnly = true)
	public List<ChunkMatch> search(ChunkSearchQuery query) {
		// 문서는 RETRIEVAL_DOCUMENT, 질의는 RETRIEVAL_QUERY 로 임베딩해야 검색이 제대로 동작한다.
		float[] queryVector = embeddingClient.embedQuery(query.query());

		List<ChunkSearchRow> rows = repository.searchByProject(
				query.projectId(),
				VectorLiteral.of(queryVector),
				query.minSimilarity(),
				query.topK());

		return rows.stream()
				.map(row -> new ChunkMatch(
						row.getChunkId(),
						row.getReferenceMaterialId(),
						row.getChunkIndex(),
						row.getContent(),
						row.getSimilarity()))
				.toList();
	}
}
