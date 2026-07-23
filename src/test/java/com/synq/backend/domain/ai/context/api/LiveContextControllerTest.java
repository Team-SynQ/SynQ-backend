package com.synq.backend.domain.ai.context.api;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.synq.backend.domain.ai.context.application.LiveContextService;
import com.synq.backend.domain.ai.context.domain.LiveContext;
import com.synq.backend.domain.ai.context.domain.LiveContextResult;
import com.synq.backend.domain.transcript.event.TranscriptFinalizedEvent;
import com.synq.backend.global.apipayload.handler.GeneralExceptionAdvice;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class LiveContextControllerTest {

	private LiveContextService liveContextService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		liveContextService = Mockito.mock(LiveContextService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new LiveContextController(liveContextService))
				.setControllerAdvice(new GeneralExceptionAdvice())
				.build();
	}

	@Test
	void 최신_회의_맥락을_조회한다() throws Exception {
		TranscriptFinalizedEvent event = new TranscriptFinalizedEvent(1L, 10L, 3, 0, 1000, "확정 전사", null);
		LiveContext context = LiveContext.create(
				1L,
				new LiveContextResult("현재까지 요약", "AI 기능", List.of("OpenAI 사용"), List.of("API 구현"), List.of()),
				event
		);
		given(liveContextService.get(1L)).willReturn(context);

		mockMvc.perform(get("/meetings/{meetingId}/live-context", 1L))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.rollingSummary").value("현재까지 요약"))
				.andExpect(jsonPath("$.result.currentTopic").value("AI 기능"))
				.andExpect(jsonPath("$.result.lastSequenceIndex").value(3));
	}
}
