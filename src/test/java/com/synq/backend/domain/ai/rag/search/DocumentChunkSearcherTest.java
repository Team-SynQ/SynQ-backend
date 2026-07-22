package com.synq.backend.domain.ai.rag.search;

import com.synq.backend.domain.ai.rag.entity.DocumentChunk;
import com.synq.backend.domain.ai.rag.repository.DocumentChunkRepository;
import com.synq.backend.support.PostgresTestContainer;
import com.synq.backend.support.StubEmbeddingClient;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentChunkSearcherTest extends PostgresTestContainer {

	private static final String MODEL = "test-model";
	private static final Long PROJECT_ID = 1L;

	@Autowired
	private DocumentChunkRepository repository;

	private DocumentChunkSearcher searcher;

	/** StubEmbeddingClient 의 질의 벡터는 항상 (1, 0, 0, ...) 이다. */
	private static float[] vector(float first, float second) {
		float[] v = new float[768];
		v[0] = first;
		v[1] = second;
		return v;
	}

	@BeforeEach
	void setUp() {
		repository.deleteAll();
		searcher = new DocumentChunkSearcher(new StubEmbeddingClient(), repository);
	}

	@Test
	void 질의와_가까운_청크를_유사도와_함께_반환한다() {
		repository.save(DocumentChunk.of(42L, PROJECT_ID, 3, "일치하는 청크", vector(1.0f, 0.0f), MODEL));
		repository.flush();

		List<ChunkMatch> matches = searcher.search(
				new ChunkSearchQuery(PROJECT_ID, "인증 방식", 5, -1.0));

		assertThat(matches).hasSize(1);
		ChunkMatch match = matches.get(0);
		assertThat(match.content()).isEqualTo("일치하는 청크");
		assertThat(match.referenceMaterialId()).isEqualTo(42L);
		assertThat(match.chunkIndex()).isEqualTo(3);
		assertThat(match.similarity()).isCloseTo(1.0, Offset.offset(0.0001));
		assertThat(match.chunkId()).isNotNull();
	}

	@Test
	void 유사도_내림차순으로_반환한다() {
		repository.save(DocumentChunk.of(1L, PROJECT_ID, 0, "먼 청크", vector(0.6f, 0.8f), MODEL));
		repository.save(DocumentChunk.of(1L, PROJECT_ID, 1, "가까운 청크", vector(1.0f, 0.0f), MODEL));
		repository.flush();

		List<ChunkMatch> matches = searcher.search(
				new ChunkSearchQuery(PROJECT_ID, "질의", 5, -1.0));

		assertThat(matches).extracting(ChunkMatch::content)
				.containsExactly("가까운 청크", "먼 청크");
	}

	@Test
	void 결과가_없으면_빈_리스트다() {
		List<ChunkMatch> matches = searcher.search(
				new ChunkSearchQuery(PROJECT_ID, "질의", 5, -1.0));

		assertThat(matches).isEmpty();
	}

	@Test
	void 빈_질의는_거부한다() {
		assertThatThrownBy(() -> new ChunkSearchQuery(PROJECT_ID, "   ", 5, -1.0))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 프로젝트_ID가_없으면_거부한다() {
		assertThatThrownBy(() -> new ChunkSearchQuery(null, "질의", 5, -1.0))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void topK가_0_이하이면_거부한다() {
		assertThatThrownBy(() -> new ChunkSearchQuery(PROJECT_ID, "질의", 0, -1.0))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
