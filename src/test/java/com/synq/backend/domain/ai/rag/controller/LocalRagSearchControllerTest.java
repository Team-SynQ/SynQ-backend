package com.synq.backend.domain.ai.rag.controller;

import com.synq.backend.domain.ai.rag.entity.DocumentChunk;
import com.synq.backend.domain.ai.rag.repository.DocumentChunkRepository;
import com.synq.backend.domain.auth.jwt.JwtProvider;
import com.synq.backend.support.PostgresTestContainer;
import com.synq.backend.support.ReferenceMaterialTestFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles("local")
class LocalRagSearchControllerTest extends PostgresTestContainer {

	private static final String MODEL = "test-model";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private DocumentChunkRepository repository;

	@Autowired
	private ReferenceMaterialTestFixture referenceMaterialTestFixture;

	@Autowired
	private JwtProvider jwtProvider;

	private ReferenceMaterialTestFixture.Fixture referenceFixture;

	/**
	 * 768차원 L2 정규화 벡터. first^2 + second^2 == 1 이어야 한다.
	 * 코사인 유사도는 방향만 보므로 정규화하지 않으면 (0.6, 0) 도 (1, 0) 과 유사도가 1.0 이 된다.
	 */
	private static float[] vector(float first, float second) {
		float[] v = new float[768];
		v[0] = first;
		v[1] = second;
		return v;
	}

	@BeforeEach
	void setUp() {
		repository.deleteAll();
		referenceFixture = referenceMaterialTestFixture.create();
	}

	// 이 클래스는 @Transactional 이 아니라 롤백되지 않는다. 남은 청크는 컨테이너를 공유하는
	// 다음 테스트 클래스의 UNIQUE(reference_material_id, chunk_index) 를 깨뜨린다.
	@AfterEach
	void tearDown() {
		repository.deleteAll();
	}

	@Test
	void 검색_결과를_반환한다() throws Exception {
		repository.save(DocumentChunk.of(
				referenceFixture.referenceMaterialId(),
				referenceFixture.projectId(),
				0,
				"인증은 JWT 로 처리한다",
				vector(1.0f, 0.0f),
				MODEL
		));
		repository.flush();

		mockMvc.perform(get("/local/rag/search")
						.header("Authorization", bearer())
						.param("projectId", referenceFixture.projectId().toString())
						.param("q", "인증 방식"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.result[0].content").value("인증은 JWT 로 처리한다"))
				.andExpect(jsonPath("$.result[0].source").value("REFERENCE_MATERIAL"))
				.andExpect(jsonPath("$.result[0].sourceId")
						.value(referenceFixture.referenceMaterialId()));
	}

	@Test
	void 다른_프로젝트의_청크는_반환하지_않는다() throws Exception {
		ReferenceMaterialTestFixture.Fixture otherReference = referenceMaterialTestFixture.create();
		// 두 청크 모두 질의와 일치하는 벡터다. projectId 파라미터만이 결과를 가른다.
		repository.save(DocumentChunk.of(
				referenceFixture.referenceMaterialId(), referenceFixture.projectId(),
				0, "1번 프로젝트 문서", vector(1.0f, 0.0f), MODEL));
		repository.save(DocumentChunk.of(
				otherReference.referenceMaterialId(), otherReference.projectId(),
				0, "2번 프로젝트 문서", vector(1.0f, 0.0f), MODEL));
		repository.flush();

		mockMvc.perform(get("/local/rag/search")
						.header("Authorization", bearer())
						.param("projectId", otherReference.projectId().toString())
						.param("q", "질의"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.length()").value(1))
				.andExpect(jsonPath("$.result[0].content").value("2번 프로젝트 문서"));
	}

	@Test
	void 임계값을_직접_넘기면_그_값으로_거른다() throws Exception {
		// StubEmbeddingClient 의 질의 벡터는 (1, 0, ...) 이므로 첫 원소가 곧 유사도다.
		repository.save(DocumentChunk.of(
				referenceFixture.referenceMaterialId(), referenceFixture.projectId(),
				0, "유사도 0.6", vector(0.6f, 0.8f), MODEL));
		repository.flush();

		mockMvc.perform(get("/local/rag/search")
						.header("Authorization", bearer())
						.param("projectId", referenceFixture.projectId().toString())
						.param("q", "질의")
						.param("minSimilarity", "0.9"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.length()").value(0));
	}

	@Test
	void 빈_질의는_400_이다() throws Exception {
		mockMvc.perform(get("/local/rag/search")
						.header("Authorization", bearer())
						.param("projectId", "1")
						.param("q", "  "))
				.andExpect(status().isBadRequest());
	}

	@Test
	void 로컬_RAG_검색도_인증_없이는_호출할_수_없다() throws Exception {
		mockMvc.perform(get("/local/rag/search")
						.param("projectId", "1")
						.param("q", "질의"))
				.andExpect(status().isUnauthorized());
	}

	private String bearer() {
		return "Bearer " + jwtProvider.createAccessToken(1L);
	}
}
