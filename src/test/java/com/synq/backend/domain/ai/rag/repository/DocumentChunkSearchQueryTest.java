package com.synq.backend.domain.ai.rag.repository;

import com.synq.backend.domain.ai.rag.entity.DocumentChunk;
import com.synq.backend.domain.ai.rag.search.ChunkSearchRow;
import com.synq.backend.domain.ai.rag.search.VectorLiteral;
import com.synq.backend.support.PostgresTestContainer;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 임베딩 API 를 호출하지 않는다. 고정된 단위 벡터를 직접 넣어 순위가 결정적으로 나오게 한다.
 */
class DocumentChunkSearchQueryTest extends PostgresTestContainer {

	private static final String MODEL = "test-model";
	private static final Long PROJECT_A = 1L;
	private static final Long PROJECT_B = 2L;

	@Autowired
	private DocumentChunkRepository repository;

	/** 768차원 L2 정규화 벡터. first^2 + second^2 == 1 이어야 한다. */
	private static float[] vector(float first, float second) {
		float[] v = new float[768];
		v[0] = first;
		v[1] = second;
		return v;
	}

	/** 질의 벡터. 이것과의 코사인 유사도는 각 벡터의 first 값과 같다. */
	private static String queryVector() {
		return VectorLiteral.of(vector(1.0f, 0.0f));
	}

	@BeforeEach
	void setUp() {
		repository.deleteAll();
	}

	// 이 클래스는 @Transactional 이 아니라 롤백되지 않는다. 남은 청크는 컨테이너를 공유하는
	// 다음 테스트 클래스의 UNIQUE(reference_material_id, chunk_index) 를 깨뜨린다.
	@AfterEach
	void tearDown() {
		repository.deleteAll();
	}

	@Test
	void 다른_프로젝트의_청크는_검색되지_않는다() {
		// 두 청크 모두 질의와 완전히 일치하는 벡터다. 스코프 조건만이 결과를 가른다.
		repository.save(DocumentChunk.of(10L, PROJECT_A, 0, "A 프로젝트 문서", vector(1.0f, 0.0f), MODEL));
		repository.save(DocumentChunk.of(20L, PROJECT_B, 0, "B 프로젝트 문서", vector(1.0f, 0.0f), MODEL));
		repository.flush();

		List<ChunkSearchRow> rows = repository.searchByProject(PROJECT_A, queryVector(), -1.0, 10);

		assertThat(rows).hasSize(1);
		assertThat(rows.get(0).getContent()).isEqualTo("A 프로젝트 문서");
		assertThat(rows.get(0).getReferenceMaterialId()).isEqualTo(10L);
	}

	@Test
	void 유사도가_높은_순으로_정렬된다() {
		repository.save(DocumentChunk.of(10L, PROJECT_A, 0, "유사도 0.6", vector(0.6f, 0.8f), MODEL));
		repository.save(DocumentChunk.of(10L, PROJECT_A, 1, "유사도 1.0", vector(1.0f, 0.0f), MODEL));
		repository.save(DocumentChunk.of(10L, PROJECT_A, 2, "유사도 0.8", vector(0.8f, 0.6f), MODEL));
		repository.flush();

		List<ChunkSearchRow> rows = repository.searchByProject(PROJECT_A, queryVector(), -1.0, 10);

		assertThat(rows).extracting(ChunkSearchRow::getContent)
				.containsExactly("유사도 1.0", "유사도 0.8", "유사도 0.6");
	}

	@Test
	void 유사도를_1에서_코사인거리를_뺀_값으로_반환한다() {
		repository.save(DocumentChunk.of(10L, PROJECT_A, 0, "청크", vector(0.6f, 0.8f), MODEL));
		repository.flush();

		List<ChunkSearchRow> rows = repository.searchByProject(PROJECT_A, queryVector(), -1.0, 10);

		assertThat(rows.get(0).getSimilarity()).isCloseTo(0.6, Offset.offset(0.0001));
	}

	@Test
	void 임계값_미만은_제외한다() {
		repository.save(DocumentChunk.of(10L, PROJECT_A, 0, "유사도 0.6", vector(0.6f, 0.8f), MODEL));
		repository.save(DocumentChunk.of(10L, PROJECT_A, 1, "유사도 1.0", vector(1.0f, 0.0f), MODEL));
		repository.flush();

		List<ChunkSearchRow> rows = repository.searchByProject(PROJECT_A, queryVector(), 0.7, 10);

		assertThat(rows).extracting(ChunkSearchRow::getContent).containsExactly("유사도 1.0");
	}

	@Test
	void topK_개수만큼만_반환한다() {
		repository.save(DocumentChunk.of(10L, PROJECT_A, 0, "유사도 0.6", vector(0.6f, 0.8f), MODEL));
		repository.save(DocumentChunk.of(10L, PROJECT_A, 1, "유사도 1.0", vector(1.0f, 0.0f), MODEL));
		repository.save(DocumentChunk.of(10L, PROJECT_A, 2, "유사도 0.8", vector(0.8f, 0.6f), MODEL));
		repository.flush();

		List<ChunkSearchRow> rows = repository.searchByProject(PROJECT_A, queryVector(), -1.0, 2);

		assertThat(rows).extracting(ChunkSearchRow::getContent)
				.containsExactly("유사도 1.0", "유사도 0.8");
	}

	@Test
	void 청크가_없는_프로젝트는_빈_결과다() {
		repository.save(DocumentChunk.of(10L, PROJECT_A, 0, "청크", vector(1.0f, 0.0f), MODEL));
		repository.flush();

		List<ChunkSearchRow> rows = repository.searchByProject(999L, queryVector(), -1.0, 10);

		assertThat(rows).isEmpty();
	}

	@Test
	void 청크_인덱스와_출처_문서_ID를_함께_반환한다() {
		// 나중에 "이 답변은 어느 문서에서 나왔는가" 를 사용자에게 보여줘야 한다.
		repository.save(DocumentChunk.of(42L, PROJECT_A, 7, "청크", vector(1.0f, 0.0f), MODEL));
		repository.flush();

		ChunkSearchRow row = repository.searchByProject(PROJECT_A, queryVector(), -1.0, 10).get(0);

		assertThat(row.getReferenceMaterialId()).isEqualTo(42L);
		assertThat(row.getChunkIndex()).isEqualTo(7);
		assertThat(row.getChunkId()).isNotNull();
	}
}
