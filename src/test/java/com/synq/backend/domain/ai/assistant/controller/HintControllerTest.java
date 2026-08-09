package com.synq.backend.domain.ai.assistant.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.synq.backend.domain.ai.assistant.domain.HintResult;
import com.synq.backend.domain.ai.assistant.domain.SegmentHint;
import com.synq.backend.domain.ai.assistant.service.HintService;
import com.synq.backend.domain.auth.jwt.CurrentUserIdResolver;
import com.synq.backend.global.config.CorsConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// CorsConfig 는 WebMvcConfigurer 라 슬라이스에 딸려오지만 CorsProperties 를 요구해 컨텍스트가 뜨지 않는다.
// 이 테스트는 컨트롤러만 검증하므로 제외하고, JWT 필터체인도 끈 뒤 CurrentUserIdResolver 를 대역으로 둔다.
@WebMvcTest(controllers = HintController.class,
		excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CorsConfig.class))
@AutoConfigureMockMvc(addFilters = false)
class HintControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	HintService hintService;

	@MockitoBean
	CurrentUserIdResolver currentUserIdResolver;

	@Test
	void 힌트를_생성해_200으로_반환한다() throws Exception {
		given(currentUserIdResolver.resolve(any())).willReturn(10L);
		given(hintService.generate(eq(10L), eq(1L), eq(3L)))
				.willReturn(new HintResult("의미다", "영향이다", "질문이다"));

		mockMvc.perform(post("/meetings/1/segments/3/hints")
						.header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.result.meaning").value("의미다"))
				.andExpect(jsonPath("$.result.myImpact").value("영향이다"))
				.andExpect(jsonPath("$.result.teamQuestion").value("질문이다"));
	}

	@Test
	void 내_힌트_목록을_200으로_반환한다() throws Exception {
		given(currentUserIdResolver.resolve(any())).willReturn(10L);
		given(hintService.getMyHints(eq(10L), eq(1L)))
				.willReturn(List.of(SegmentHint.of(1L, 3L, 10L,
						new HintResult("의미다", "영향이다", "질문이다"))));

		mockMvc.perform(get("/meetings/1/hints")
						.header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.result.meetingId").value(1))
				.andExpect(jsonPath("$.result.hints[0].segmentId").value(3))
				.andExpect(jsonPath("$.result.hints[0].meaning").value("의미다"))
				.andExpect(jsonPath("$.result.hints[0].myImpact").value("영향이다"))
				.andExpect(jsonPath("$.result.hints[0].teamQuestion").value("질문이다"));
	}

	@Test
	void 힌트가_없으면_빈_배열을_반환한다() throws Exception {
		given(currentUserIdResolver.resolve(any())).willReturn(10L);
		given(hintService.getMyHints(eq(10L), eq(1L))).willReturn(List.of());

		mockMvc.perform(get("/meetings/1/hints")
						.header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.hints").isArray())
				.andExpect(jsonPath("$.result.hints").isEmpty());
	}
}
