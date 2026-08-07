package com.synq.backend.domain.ai.rag.controller;

import com.synq.backend.domain.auth.jwt.JwtProvider;
import com.synq.backend.support.PostgresTestContainer;
import com.synq.backend.support.ReferenceMaterialTestFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class DocumentReindexControllerTest extends PostgresTestContainer {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtProvider jwtProvider;

	@Autowired
	private ReferenceMaterialTestFixture referenceMaterialTestFixture;

	@Test
	void 존재하지_않는_문서를_재처리하면_404_다() throws Exception {
		ReferenceMaterialTestFixture.Fixture fixture = referenceMaterialTestFixture.create();

		mockMvc.perform(post("/reference-materials/{id}/reindex", 999L)
						.header(HttpHeaders.AUTHORIZATION, bearer(fixture.uploaderId())))
				.andExpect(status().isNotFound());
	}

	@Test
	void 인증_없이는_재인덱싱할_수_없다() throws Exception {
		// 임베딩 API 호출과 청크 재생성을 유발하므로 인증 없이 열어두면 비용 유발에 노출된다.
		mockMvc.perform(post("/reference-materials/{id}/reindex", 999L))
				.andExpect(status().isUnauthorized());
	}

	private String bearer(Long userId) {
		return "Bearer " + jwtProvider.createAccessToken(userId);
	}
}
