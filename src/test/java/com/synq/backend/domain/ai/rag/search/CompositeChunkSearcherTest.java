package com.synq.backend.domain.ai.rag.search;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CompositeChunkSearcherTest {

	private static ChunkMatch match(ChunkSource source, long sourceId, String content, double similarity) {
		return new ChunkMatch(sourceId * 100, source, sourceId, 0, content, similarity);
	}

	/** 고정 결과를 돌려주는 대역. DB 없이 합류 로직만 검증한다. */
	private static ChunkSearcher stub(List<ChunkMatch> results) {
		return query -> results;
	}

	@Test
	void 두_소스를_유사도_내림차순으로_합친다() {
		CompositeChunkSearcher searcher = new CompositeChunkSearcher(
				stub(List.of(
						match(ChunkSource.REFERENCE_MATERIAL, 1L, "문서 상", 0.9),
						match(ChunkSource.REFERENCE_MATERIAL, 2L, "문서 하", 0.5))),
				stub(List.of(
						match(ChunkSource.MEETING_TRANSCRIPT, 3L, "회의 중", 0.7))));

		List<ChunkMatch> matches = searcher.search(new ChunkSearchQuery(1L, "질의", 5, -1.0));

		assertThat(matches).extracting(ChunkMatch::content)
				.containsExactly("문서 상", "회의 중", "문서 하");
	}

	@Test
	void 합친_뒤_topK_로_자른다() {
		// 각 검색기가 이미 topK 로 잘라 오므로, 합친 뒤 다시 잘라야 전체 기준 상위 K 가 된다.
		CompositeChunkSearcher searcher = new CompositeChunkSearcher(
				stub(List.of(match(ChunkSource.REFERENCE_MATERIAL, 1L, "문서", 0.9))),
				stub(List.of(match(ChunkSource.MEETING_TRANSCRIPT, 2L, "회의", 0.8))));

		List<ChunkMatch> matches = searcher.search(new ChunkSearchQuery(1L, "질의", 1, -1.0));

		assertThat(matches).hasSize(1);
		assertThat(matches.get(0).content()).isEqualTo("문서");
	}

	@Test
	void 출처_구분이_보존된다() {
		CompositeChunkSearcher searcher = new CompositeChunkSearcher(
				stub(List.of(match(ChunkSource.REFERENCE_MATERIAL, 1L, "문서", 0.9))),
				stub(List.of(match(ChunkSource.MEETING_TRANSCRIPT, 2L, "회의", 0.8))));

		List<ChunkMatch> matches = searcher.search(new ChunkSearchQuery(1L, "질의", 5, -1.0));

		assertThat(matches).extracting(ChunkMatch::source)
				.containsExactly(ChunkSource.REFERENCE_MATERIAL, ChunkSource.MEETING_TRANSCRIPT);
	}

	@Test
	void 한쪽이_비어도_다른_쪽_결과를_돌려준다() {
		CompositeChunkSearcher searcher = new CompositeChunkSearcher(
				stub(List.of()),
				stub(List.of(match(ChunkSource.MEETING_TRANSCRIPT, 2L, "회의", 0.8))));

		List<ChunkMatch> matches = searcher.search(new ChunkSearchQuery(1L, "질의", 5, -1.0));

		assertThat(matches).extracting(ChunkMatch::content).containsExactly("회의");
	}
}
