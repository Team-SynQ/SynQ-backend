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
import com.synq.backend.domain.ai.summary.mock.InMemoryPersonalSummaryStore;
import com.synq.backend.domain.ai.summary.mock.InMemorySummaryJobStore;
import com.synq.backend.domain.ai.summary.mock.MockRagContextReader;
import com.synq.backend.domain.ai.summary.mock.MockTranscriptReader;
import com.synq.backend.domain.ai.summary.application.PersonalSummaryQueryService;
import com.synq.backend.domain.ai.summary.domain.PersonalSummaryTarget;
import com.synq.backend.domain.auth.jwt.CurrentUserIdResolver;
import org.mockito.Mockito;
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
		var personalSummaryStore = new InMemoryPersonalSummaryStore();
		var contextBuilder = new SummaryContextBuilder(
				new MockTranscriptReader(),
				new MockRagContextReader(),
				new com.synq.backend.domain.ai.summary.application.SummaryProperties(
						"test-model", "test-v1", 600_000));
		var fakeClient = new FakeSummaryAiClient();
		var properties = new com.synq.backend.domain.ai.summary.application.SummaryProperties(
				"test-model", "test-v1", 600_000);
		var processor = new SummaryJobProcessor(
				jobStore,
				contextBuilder,
				fakeClient,
				fakeClient,
				meetingId -> java.util.List.of(new PersonalSummaryTarget(
						7L, "DEV_TECH - 백엔드", java.util.List.of("TECH_RISK"))),
				new com.synq.backend.domain.ai.summary.application.SummaryResultWriter(
						summaryStore, personalSummaryStore, jobStore),
				properties,
				event -> {
				}
		);
		// 이 테스트는 요약 파이프라인 자체를 검증하므로 회의는 항상 종료된 것으로 간주한다.
		var accessValidator = Mockito.mock(
				com.synq.backend.domain.ai.summary.application.SummaryAccessValidator.class);
		var service = new MeetingSummaryService(
				jobStore,
				summaryStore,
				processor,
				meetingId -> true,
				meetingId -> java.util.Optional.of("테스트 회의"),
				properties,
				accessValidator,
				event -> {
				}
		);
		CurrentUserIdResolver userIdResolver = Mockito.mock(CurrentUserIdResolver.class);
		Mockito.when(userIdResolver.resolve("Bearer test-token")).thenReturn(7L);
		mockMvc = MockMvcBuilders.standaloneSetup(new AiSummaryController(
				service,
				new PersonalSummaryQueryService(personalSummaryStore),
				userIdResolver
		)).build();
	}

	@Test
	void Mock_데이터로_요약_생성부터_조회까지_수행한다() throws Exception {
		MvcResult generated = mockMvc.perform(post("/meetings/{meetingId}/ai-summary/generate", 1L)
						.header("Authorization", "Bearer test-token"))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.result.status").value("QUEUED"))
				.andReturn();

		String jobId = JsonPath.read(generated.getResponse().getContentAsString(), "$.result.jobId");
		String jobStatus = waitForCompletion(jobId);
		assertThat(jobStatus).isEqualTo("COMPLETED");

		mockMvc.perform(get("/meetings/{meetingId}/summary", 1L)
					.header("Authorization", "Bearer test-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.title").value("테스트 회의"))
				.andExpect(jsonPath("$.result.oneLineSummary").isNotEmpty())
				.andExpect(jsonPath("$.result.keyTopics").isArray())
				.andExpect(jsonPath("$.result.discussionSections").isArray())
				.andExpect(jsonPath("$.result.discussionSections[0].title").isNotEmpty())
				.andExpect(jsonPath("$.result.decisions").isArray())
				.andExpect(jsonPath("$.result.tentativeDirections").isArray())
				.andExpect(jsonPath("$.result.confirmationItems").isArray())
				.andExpect(jsonPath("$.result.confirmationItems[0]").value("API 명세 초안을 작성한다."))
				.andExpect(jsonPath("$.result.overallSummary").doesNotExist())
				.andExpect(jsonPath("$.result.actionItems").doesNotExist())
				.andExpect(jsonPath("$.result.openQuestions").doesNotExist());

		mockMvc.perform(get("/meetings/{meetingId}/summary/me", 1L)
						.header("Authorization", "Bearer test-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.userId").value(7))
				.andExpect(jsonPath("$.result.role").value("DEV_TECH - 백엔드"))
				.andExpect(jsonPath("$.result.personalSummary").isNotEmpty());
	}

	private String waitForCompletion(String jobId) throws Exception {
		for (int attempt = 0; attempt < 20; attempt++) {
			MvcResult result = mockMvc.perform(get("/meetings/{meetingId}/ai-summary/status", 1L)
						.header("Authorization", "Bearer test-token")
						.queryParam("jobId", jobId))
					.andExpect(status().isOk())
					.andReturn();
			String jobStatus = JsonPath.read(result.getResponse().getContentAsString(), "$.result.status");
			if ("COMPLETED".equals(jobStatus)
					|| "COMPLETED_WITH_ERRORS".equals(jobStatus)
					|| "FAILED".equals(jobStatus)) {
				return jobStatus;
			}
			Thread.sleep(50);
		}
		return "TIMED_OUT";
	}
}
