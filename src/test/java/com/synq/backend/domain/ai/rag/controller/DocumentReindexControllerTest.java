package com.synq.backend.domain.ai.rag.controller;

import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class DocumentReindexControllerTest extends PostgresTestContainer {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void 존재하지_않는_문서를_재처리하면_404_다() throws Exception {
		mockMvc.perform(post("/reference-materials/{id}/reindex", 999L))
				.andExpect(status().isNotFound());
	}
}
