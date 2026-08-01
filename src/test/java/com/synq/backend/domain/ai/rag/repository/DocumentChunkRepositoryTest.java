package com.synq.backend.domain.ai.rag.repository;

import com.synq.backend.domain.ai.rag.entity.DocumentChunk;
import com.synq.backend.support.PostgresTestContainer;
import com.synq.backend.support.ReferenceMaterialTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class DocumentChunkRepositoryTest extends PostgresTestContainer {

	private static final String MODEL = "gemini-embedding-001";

	@Autowired
	private DocumentChunkRepository repository;

	@Autowired
	private ReferenceMaterialTestFixture referenceMaterialTestFixture;

	private ReferenceMaterialTestFixture.Fixture referenceFixture;

	/** 첫 원소만 지정한 768차원 벡터. */
	private static float[] vector(float first) {
		float[] v = new float[768];
		v[0] = first;
		return v;
	}

	@BeforeEach
	void setUp() {
		referenceFixture = referenceMaterialTestFixture.create();
	}

	@Test
	void 청크와_벡터를_저장하고_조회한다() {
		repository.save(DocumentChunk.of(
				referenceFixture.referenceMaterialId(),
				referenceFixture.projectId(),
				0,
				"첫 번째 청크",
				vector(1.0f),
				MODEL
		));
		repository.save(DocumentChunk.of(
				referenceFixture.referenceMaterialId(),
				referenceFixture.projectId(),
				1,
				"두 번째 청크",
				vector(0.5f),
				MODEL
		));
		repository.flush();

		List<DocumentChunk> chunks = repository.findByReferenceMaterialIdOrderByChunkIndexAsc(
				referenceFixture.referenceMaterialId());

		assertThat(chunks).hasSize(2);
		assertThat(chunks.get(0).getContent()).isEqualTo("첫 번째 청크");
		assertThat(chunks.get(0).getEmbedding()).hasSize(768);
		assertThat(chunks.get(0).getEmbedding()[0]).isEqualTo(1.0f);
		assertThat(chunks.get(0).getEmbeddingModel()).isEqualTo(MODEL);
		assertThat(chunks.get(1).getChunkIndex()).isEqualTo(1);
	}

	@Test
	void 문서의_청크를_전부_삭제한다() {
		Long otherReferenceId = referenceMaterialTestFixture.createReference(referenceFixture);
		repository.save(DocumentChunk.of(
				referenceFixture.referenceMaterialId(),
				referenceFixture.projectId(),
				0,
				"지워질 청크",
				vector(1.0f),
				MODEL
		));
		repository.save(DocumentChunk.of(
				otherReferenceId,
				referenceFixture.projectId(),
				0,
				"남을 청크",
				vector(1.0f),
				MODEL
		));
		repository.flush();

		repository.deleteByReferenceMaterialId(referenceFixture.referenceMaterialId());
		repository.flush();

		assertThat(repository.findByReferenceMaterialIdOrderByChunkIndexAsc(
				referenceFixture.referenceMaterialId())).isEmpty();
		assertThat(repository.findByReferenceMaterialIdOrderByChunkIndexAsc(otherReferenceId)).hasSize(1);
	}

	@Test
	void 청크에_검색_스코프인_프로젝트_ID가_저장된다() {
		repository.save(DocumentChunk.of(
				referenceFixture.referenceMaterialId(),
				referenceFixture.projectId(),
				0,
				"청크",
				vector(1.0f),
				MODEL
		));
		repository.flush();

		DocumentChunk saved = repository.findByReferenceMaterialIdOrderByChunkIndexAsc(
				referenceFixture.referenceMaterialId()).get(0);

		assertThat(saved.getProjectId()).isEqualTo(referenceFixture.projectId());
	}
}
