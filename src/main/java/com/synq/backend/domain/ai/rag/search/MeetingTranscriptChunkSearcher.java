package com.synq.backend.domain.ai.rag.search;

import com.synq.backend.domain.ai.client.EmbeddingClient;
import com.synq.backend.domain.ai.rag.repository.MeetingTranscriptChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 과거 회의 전사 청크(meeting_transcript_chunk)를 대상으로 검색한다.
 * 검색 스코프가 프로젝트이므로 같은 프로젝트의 여러 회의가 함께 나온다.
 */
@Component
@RequiredArgsConstructor
public class MeetingTranscriptChunkSearcher implements ChunkSearcher {

	private final EmbeddingClient embeddingClient;
	private final MeetingTranscriptChunkRepository repository;

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
						ChunkSource.MEETING_TRANSCRIPT,
						row.getSourceId(),
						row.getChunkIndex(),
						row.getContent(),
						row.getSimilarity()))
				.toList();
	}
}
