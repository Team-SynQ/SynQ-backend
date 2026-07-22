package com.synq.backend.domain.ai.rag.controller;

import com.synq.backend.domain.ai.rag.entity.DocumentChunk;
import com.synq.backend.domain.ai.rag.repository.DocumentChunkRepository;
import com.synq.backend.support.PostgresTestContainer;
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
	}

	@Test
	void 검색_결과를_반환한다() throws Exception {
		repository.save(DocumentChunk.of(42L, 1L, 0, "인증은 JWT 로 처리한다", vector(1.0f, 0.0f), MODEL));
		repository.flush();

		mockMvc.perform(get("/local/rag/search")
						.param("projectId", "1")
						.param("q", "인증 방식"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.result[0].content").value("인증은 JWT 로 처리한다"))
				.andExpect(jsonPath("$.result[0].referenceMaterialId").value(42));
	}

	@Test
	void 다른_프로젝트의_청크는_반환하지_않는다() throws Exception {
		// 두 청크 모두 질의와 일치하는 벡터다. projectId 파라미터만이 결과를 가른다.
		repository.save(DocumentChunk.of(10L, 1L, 0, "1번 프로젝트 문서", vector(1.0f, 0.0f), MODEL));
		repository.save(DocumentChunk.of(20L, 2L, 0, "2번 프로젝트 문서", vector(1.0f, 0.0f), MODEL));
		repository.flush();

		mockMvc.perform(get("/local/rag/search")
						.param("projectId", "2")
						.param("q", "질의"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.length()").value(1))
				.andExpect(jsonPath("$.result[0].content").value("2번 프로젝트 문서"));
	}

	@Test
	void 임계값을_직접_넘기면_그_값으로_거른다() throws Exception {
		// StubEmbeddingClient 의 질의 벡터는 (1, 0, ...) 이므로 첫 원소가 곧 유사도다.
		repository.save(DocumentChunk.of(10L, 1L, 0, "유사도 0.6", vector(0.6f, 0.8f), MODEL));
		repository.flush();

		mockMvc.perform(get("/local/rag/search")
						.param("projectId", "1")
						.param("q", "질의")
						.param("minSimilarity", "0.9"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.length()").value(0));
	}

	@Test
	void 빈_질의는_400_이다() throws Exception {
		mockMvc.perform(get("/local/rag/search")
						.param("projectId", "1")
						.param("q", "  "))
				.andExpect(status().isBadRequest());
	}
}
