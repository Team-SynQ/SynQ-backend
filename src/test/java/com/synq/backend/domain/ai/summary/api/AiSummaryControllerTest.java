package com.synq.backend.domain.ai.summary.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.synq.backend.domain.ai.summary.application.MeetingSummaryService;
import com.synq.backend.domain.ai.summary.application.SummaryContextBuilder;
import com.synq.backend.domain.ai.summary.application.SummaryJobProcessor;
import com.synq.backend.domain.ai.summary.mock.FakeSummaryAiClient;
import com.synq.backend.domain.ai.summary.mock.InMemoryMeetingSummaryStore;
import com.synq.backend.domain.ai.summary.mock.InMemorySummaryJobStore;
import com.synq.backend.domain.ai.summary.mock.MockMeetingContextReader;
import com.synq.backend.domain.ai.summary.mock.MockRagContextReader;
import com.synq.backend.domain.ai.summary.mock.MockTranscriptReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AiSummaryControllerTest {

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		var jobStore = new InMemorySummaryJobStore();
		var summaryStore = new InMemoryMeetingSummaryStore();
		var contextBuilder = new SummaryContextBuilder(
				new MockTranscriptReader(), new MockMeetingContextReader(), new MockRagContextReader());
		var processor = new SummaryJobProcessor(jobStore, summaryStore, contextBuilder, new FakeSummaryAiClient(), event -> {});
		// 이 테스트는 요약 파이프라인 자체를 검증하므로 회의는 항상 종료된 것으로 간주한다.
		var service = new MeetingSummaryService(jobStore, summaryStore, processor, meetingId -> true);
		mockMvc = MockMvcBuilders.standaloneSetup(new AiSummaryController(service)).build();
	}

	@Test
	void Mock_데이터로_요약_생성부터_조회까지_수행한다() throws Exception {
		MvcResult generated = mockMvc.perform(post("/meetings/{meetingId}/ai-summary/generate", 1L))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.result.status").value("QUEUED"))
				.andReturn();

		String jobId = JsonPath.read(generated.getResponse().getContentAsString(), "$.result.jobId");
		String jobStatus = waitForCompletion(jobId);
		assertThat(jobStatus).isEqualTo("COMPLETED");

		mockMvc.perform(get("/meetings/{meetingId}/summary", 1L))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.overallSummary").isNotEmpty())
				.andExpect(jsonPath("$.result.actionItems[0]").value("API 명세 초안을 작성한다."));
	}

	private String waitForCompletion(String jobId) throws Exception {
		for (int attempt = 0; attempt < 20; attempt++) {
			MvcResult result = mockMvc.perform(get("/meetings/{meetingId}/ai-summary/status", 1L)
						.queryParam("jobId", jobId))
					.andExpect(status().isOk())
					.andReturn();
			String jobStatus = JsonPath.read(result.getResponse().getContentAsString(), "$.result.status");
			if ("COMPLETED".equals(jobStatus) || "FAILED".equals(jobStatus)) {
				return jobStatus;
			}
			Thread.sleep(50);
		}
		return "TIMED_OUT";
	}
}
