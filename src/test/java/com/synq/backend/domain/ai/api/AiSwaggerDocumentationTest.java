package com.synq.backend.domain.ai.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.synq.backend.domain.ai.assistant.api.AiChatControllerDocs;
import com.synq.backend.domain.ai.assistant.api.dto.AiChatSendRequest;
import com.synq.backend.domain.ai.assistant.controller.HintControllerDocs;
import com.synq.backend.domain.ai.context.api.LiveContextController;
import com.synq.backend.domain.ai.event.api.AiEventControllerDocs;
import com.synq.backend.domain.ai.rag.controller.DocumentReindexController;
import com.synq.backend.domain.ai.summary.api.AiSummaryController;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class AiSwaggerDocumentationTest {

	@Test
	void AI_API의_성공_상태와_인증_SSE_계약이_Swagger_어노테이션에_명시된다() throws Exception {
		assertThat(responseCodes(AiChatControllerDocs.class.getMethod(
				"send", Long.class, String.class, AiChatSendRequest.class)))
				.contains("200", "201", "401", "403", "409", "422", "502");
		assertThat(AiChatControllerDocs.class.getAnnotation(SecurityRequirement.class).name())
				.isEqualTo("bearerAuth");

		assertThat(responseCodes(AiSummaryController.class.getMethod("generate", Long.class, String.class)))
				.contains("202", "401", "403", "404", "409", "503");
		assertThat(AiSummaryController.class.getAnnotation(SecurityRequirement.class).name())
				.isEqualTo("bearerAuth");

		Method subscribe = AiEventControllerDocs.class.getMethod("subscribe", Long.class, String.class);
		assertThat(responseCodes(subscribe)).contains("200", "401", "403", "404");
		assertThat(AiEventControllerDocs.class.getAnnotation(SecurityRequirement.class).name())
				.isEqualTo("bearerAuth");
		assertThat(Arrays.stream(subscribe.getAnnotation(ApiResponses.class).value())
				.filter(response -> "200".equals(response.responseCode()))
				.flatMap(response -> Arrays.stream(response.content()))
				.map(content -> content.mediaType()))
				.contains("text/event-stream");

		assertThat(HintControllerDocs.class.getAnnotation(SecurityRequirement.class).name())
				.isEqualTo("bearerAuth");
		assertThat(LiveContextController.class.getAnnotation(Tag.class).name()).isEqualTo("Live Context");
		assertThat(DocumentReindexController.class.getAnnotation(Tag.class).name()).isEqualTo("RAG");
	}

	private Set<String> responseCodes(Method method) {
		return Arrays.stream(method.getAnnotation(ApiResponses.class).value())
				.map(response -> response.responseCode())
				.collect(Collectors.toSet());
	}
}
