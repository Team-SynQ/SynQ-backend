package com.synq.backend.domain.ai.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synq.backend.domain.ai.assistant.api.AiChatController;
import com.synq.backend.domain.ai.assistant.application.AiChatService;
import com.synq.backend.domain.ai.assistant.controller.HintController;
import com.synq.backend.domain.ai.assistant.service.HintService;
import com.synq.backend.domain.ai.event.AiEventSubscriptionService;
import com.synq.backend.domain.ai.event.api.AiEventController;
import com.synq.backend.domain.ai.summary.api.AiSummaryController;
import com.synq.backend.domain.ai.summary.application.MeetingSummaryService;
import com.synq.backend.domain.ai.summary.application.PersonalSummaryQueryService;
import com.synq.backend.domain.auth.jwt.CurrentUserIdResolver;
import com.synq.backend.global.config.CorsConfig;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;

@WebMvcTest(
		controllers = {AiChatController.class, HintController.class, AiEventController.class, AiSummaryController.class},
		excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CorsConfig.class)
)
@Import({SpringDocConfiguration.class, SpringDocConfigProperties.class, SpringDocWebMvcConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class AiOpenApiContractTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private AiChatService aiChatService;

	@MockitoBean
	private HintService hintService;

	@MockitoBean
	private AiEventSubscriptionService aiEventSubscriptionService;

	@MockitoBean
	private MeetingSummaryService meetingSummaryService;

	@MockitoBean
	private PersonalSummaryQueryService personalSummaryQueryService;

	@MockitoBean
	private CurrentUserIdResolver currentUserIdResolver;

	@Test
	void 생성된_OpenAPI가_AI_API의_실제_계약을_포함한다() throws Exception {
		String document = mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		JsonNode root = objectMapper.readTree(document);

		JsonNode hintGeneration = root.at("/paths/~1meetings~1{meetingId}~1segments~1{segmentId}~1hints/post");
		assertThat(hintGeneration.path("responses").has("502")).isTrue();
		assertThat(hintGeneration.path("description").asText())
				.contains("수동 생성 API", "hint.auto-created");

		JsonNode eventSubscription = root.at("/paths/~1meetings~1{meetingId}~1ai-events/get");
		assertThat(eventSubscription.path("description").asText())
				.contains("connected", "heartbeat", "live-context.updated", "hint.auto-created",
						"summary.completed", "summary.failed", "대상 사용자의 연결에만");
		JsonNode eventExamples = eventSubscription.at("/responses/200/content/text~1event-stream/examples");
		assertThat(eventExamples.size()).isEqualTo(6);
		assertThat(allExampleValues(eventExamples))
				.contains("CONNECTED", "HEARTBEAT", "LIVE_CONTEXT_UPDATED", "AUTO_HINT_CREATED",
						"SUMMARY_COMPLETED", "SUMMARY_FAILED");

		JsonNode summaryJob = root.at("/components/schemas/SummaryJobResponse/properties");
		assertThat(stringValues(summaryJob.path("status").path("enum")))
				.containsExactly("QUEUED", "PROCESSING", "COMPLETED", "COMPLETED_WITH_ERRORS", "FAILED");
		assertThat(summaryJob.path("status").path("description").asText()).contains("부분 성공");
		assertThat(summaryJob.path("failedPersonalSummaryCount").path("description").asText())
				.contains("COMPLETED_WITH_ERRORS");
		assertThat(summaryJob.path("completedAt").path("description").asText())
				.contains("QUEUED", "PROCESSING", "null");

		JsonNode chatRequest = root.at("/components/schemas/AiChatSendRequest");
		assertThat(stringValues(chatRequest.path("required"))).contains("question", "clientRequestId");
		assertThat(chatRequest.at("/properties/question/minLength").asInt()).isEqualTo(1);
		assertThat(chatRequest.at("/properties/question/maxLength").asInt()).isEqualTo(2000);
		assertThat(chatRequest.at("/properties/linkedSegmentId/description").asText()).contains("생략");
		assertThat(chatRequest.at("/properties/clientRequestId/description").asText()).contains("멱등성 키");
	}

	private String allExampleValues(JsonNode examples) {
		StringBuilder values = new StringBuilder();
		examples.elements().forEachRemaining(example -> values.append(example.path("value")));
		return values.toString();
	}

	private List<String> stringValues(JsonNode array) {
		List<String> values = new ArrayList<>();
		array.elements().forEachRemaining(value -> values.add(value.asText()));
		return values;
	}
}
