package com.synq.backend.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ApiErrorResponseDocumentationTest {

	@Test
	void 프로젝트와_회의_API의_오류_응답이_문서화된다() throws Exception {
		Class<?> projectDocs = Class.forName(
				"com.synq.backend.domain.project.controller.ProjectControllerDocs");
		assertThat(responseCodes(method(projectDocs, "create")))
				.contains("201", "400", "401", "404", "409");
		assertThat(responseCodes(method(projectDocs, "createJoinRequest")))
				.contains("201", "400", "401", "404", "409", "410", "500");

		Class<?> meetingDocs = Class.forName(
				"com.synq.backend.domain.meeting.controller.MeetingControllerDocs");
		assertThat(responseCodes(method(meetingDocs, "create")))
				.contains("201", "400", "401", "403", "409");
		assertThat(responseCodes(method(meetingDocs, "findAll")))
				.contains("200", "401", "403");
	}

	@Test
	void 네이버_state_발급_실패_응답이_문서화된다() throws Exception {
		Class<?> naverDocs = Class.forName(
				"com.synq.backend.domain.auth.controller.NaverAuthControllerDocs");

		assertThat(responseCodes(method(naverDocs, "issueState")))
				.contains("200", "500");
	}

	private Method method(Class<?> type, String name) {
		return Arrays.stream(type.getDeclaredMethods())
				.filter(method -> method.getName().equals(name))
				.findFirst()
				.orElseThrow();
	}

	private Set<String> responseCodes(Method method) {
		return Arrays.stream(method.getAnnotation(ApiResponses.class).value())
				.map(response -> response.responseCode())
				.collect(Collectors.toSet());
	}
}
