package com.synq.backend.domain.ai.assistant.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.synq.backend.domain.ai.assistant.domain.HintResult;
import com.synq.backend.domain.ai.assistant.service.HintService;
import com.synq.backend.global.config.CorsConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// CorsConfig 는 WebMvcConfigurer 라 슬라이스에 딸려오지만 CorsProperties 를 요구해 컨텍스트가 뜨지 않는다.
// 이 테스트는 컨트롤러만 검증하므로 제외한다.
@WebMvcTest(controllers = HintController.class,
		excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CorsConfig.class))
class HintControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	HintService hintService;

	@Test
	void 힌트를_생성해_200으로_반환한다() throws Exception {
		given(hintService.generate(eq(10L), eq(1L), eq(3L)))
				.willReturn(new HintResult("의미다", "영향이다", "질문이다"));

		mockMvc.perform(post("/meetings/1/segments/3/hints")
						.header("X-User-Id", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.result.meaning").value("의미다"))
				.andExpect(jsonPath("$.result.myImpact").value("영향이다"))
				.andExpect(jsonPath("$.result.teamQuestion").value("질문이다"));
	}
}
