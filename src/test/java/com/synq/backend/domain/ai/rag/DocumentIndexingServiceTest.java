package com.synq.backend.domain.ai.rag;

import com.synq.backend.domain.ai.rag.chunking.TextChunker;
import com.synq.backend.domain.ai.rag.entity.DocumentChunk;
import com.synq.backend.domain.ai.rag.repository.DocumentChunkRepository;
import com.synq.backend.support.PostgresTestContainer;
import com.synq.backend.support.ReferenceMaterialTestFixture;
import com.synq.backend.support.StubEmbeddingClient;
import com.synq.backend.support.StubReferenceMaterialPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentIndexingServiceTest extends PostgresTestContainer {

	@Autowired
	private DocumentChunkRepository repository;

	@Autowired
	private ReferenceMaterialTestFixture referenceMaterialTestFixture;

	// 직접 new 하면 @Transactional 프록시가 없어 파생 삭제 쿼리가 실패한다. 반드시 빈으로 받는다.
	@Autowired
	private DocumentChunkWriter chunkWriter;

	private StubEmbeddingClient embeddingClient;
	private StubReferenceMaterialPort port;
	private DocumentIndexingService service;
	private ReferenceMaterialTestFixture.Fixture referenceFixture;

	@BeforeEach
	void setUp() {
		repository.deleteAll();
		referenceFixture = referenceMaterialTestFixture.create();
		embeddingClient = new StubEmbeddingClient();
		port = new StubReferenceMaterialPort();
		service = new DocumentIndexingService(
				new TextChunker(800, 100), embeddingClient, chunkWriter, port);
	}

	// 이 클래스는 @Transactional 이 아니라 롤백되지 않는다. 남은 청크는 컨테이너를 공유하는
	// 다음 테스트 클래스의 UNIQUE(reference_material_id, chunk_index) 를 깨뜨린다.
	@AfterEach
	void tearDown() {
		repository.deleteAll();
	}

	/** 800자를 넘겨 청크가 여러 개 나오도록 하는 텍스트. */
	private static String longText() {
		return ("가".repeat(500) + "\n\n" + "나".repeat(500) + "\n\n" + "다".repeat(500));
	}

	@Test
	void 청크를_저장하고_COMPLETED_로_전이한다() {
		service.index(referenceFixture.referenceMaterialId(), referenceFixture.projectId(), longText());

		List<DocumentChunk> chunks = repository.findByReferenceMaterialIdOrderByChunkIndexAsc(
				referenceFixture.referenceMaterialId());

		assertThat(chunks).hasSizeGreaterThan(1);
		assertThat(chunks.get(0).getChunkIndex()).isZero();
		assertThat(chunks.get(1).getChunkIndex()).isEqualTo(1);
		assertThat(chunks.get(0).getEmbedding()).hasSize(768);
		assertThat(chunks.get(0).getEmbeddingModel()).isEqualTo("stub-embedding-model");
		assertThat(port.statusOf(referenceFixture.referenceMaterialId())).isEqualTo("COMPLETED");
	}

	@Test
	void 임베딩이_실패하면_청크를_남기지_않고_FAILED_로_전이한다() {
		embeddingClient.failNext();

		// 동기 호출자는 실패를 예외로 전달받는다. 그 와중에도 청크 정리와 FAILED 전이는 이뤄진다.
		assertThatThrownBy(() -> service.index(
				referenceFixture.referenceMaterialId(),
				referenceFixture.projectId(),
				longText()
		))
				.isInstanceOf(RuntimeException.class);

		assertThat(repository.findByReferenceMaterialIdOrderByChunkIndexAsc(
				referenceFixture.referenceMaterialId())).isEmpty();
		assertThat(port.statusOf(referenceFixture.referenceMaterialId())).isEqualTo("FAILED");
		assertThat(port.failureReasonOf(referenceFixture.referenceMaterialId())).isNotBlank();
	}

	@Test
	void 재처리는_기존_청크를_지우고_다시_만든다() {
		service.index(referenceFixture.referenceMaterialId(), referenceFixture.projectId(), longText());
		int firstCount = repository.findByReferenceMaterialIdOrderByChunkIndexAsc(
				referenceFixture.referenceMaterialId()).size();

		service.index(referenceFixture.referenceMaterialId(), referenceFixture.projectId(), longText());
		List<DocumentChunk> chunks = repository.findByReferenceMaterialIdOrderByChunkIndexAsc(
				referenceFixture.referenceMaterialId());

		// 멱등: 두 번 돌려도 청크 수가 같고 중복이 없다 (UNIQUE 제약 위반도 나지 않는다)
		assertThat(chunks).hasSize(firstCount);
		assertThat(port.statusOf(referenceFixture.referenceMaterialId())).isEqualTo("COMPLETED");
	}

	@Test
	void 텍스트가_비어_있으면_FAILED_로_전이한다() {
		assertThatThrownBy(() -> service.index(
				referenceFixture.referenceMaterialId(),
				referenceFixture.projectId(),
				"   "
		))
				.isInstanceOf(RuntimeException.class);

		assertThat(repository.findByReferenceMaterialIdOrderByChunkIndexAsc(
				referenceFixture.referenceMaterialId())).isEmpty();
		assertThat(port.statusOf(referenceFixture.referenceMaterialId())).isEqualTo("FAILED");
	}

	@Test
	void 저장된_청크에_검색_스코프인_프로젝트_ID가_들어간다() {
		service.index(referenceFixture.referenceMaterialId(), referenceFixture.projectId(), longText());

		List<DocumentChunk> chunks = repository.findByReferenceMaterialIdOrderByChunkIndexAsc(
				referenceFixture.referenceMaterialId());

		assertThat(chunks).isNotEmpty();
		assertThat(chunks).allSatisfy(chunk ->
				assertThat(chunk.getProjectId()).isEqualTo(referenceFixture.projectId()));
	}
}
