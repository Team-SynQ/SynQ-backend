package com.synq.backend.domain.project.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectJoinSettingSource;
import com.synq.backend.domain.project.entity.ProjectParticipationRequest;
import com.synq.backend.domain.project.repository.ProjectParticipationRequestRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.user.entity.Role;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class ProjectJoinRequestResultControllerTest extends ProjectControllerTestSupport {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectParticipationRequestRepository participationRequestRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManager entityManager;

	@Test
	void 로그인_사용자는_자신의_처리된_참여_요청_결과를_200으로_조회한다() throws Exception {
		User requester = saveUser("요청자", "result-controller@synq.com");
		Project approvedProject = saveProject(requester, "SynQ");
		Project rejectedProject = saveProject(requester, "UMC");
		ProjectParticipationRequest approved = savePending(approvedProject, requester);
		ProjectParticipationRequest rejected = savePending(rejectedProject, requester);
		approved.approve();
		rejected.reject();
		participationRequestRepository.flush();
		setUpdatedAt(approved.getId(), LocalDateTime.of(2026, 8, 12, 14, 10));
		setUpdatedAt(rejected.getId(), LocalDateTime.of(2026, 8, 13, 14, 10));
		entityManager.clear();

		MvcResult result = mockMvc.perform(get("/projects/join-requests/me")
						.header(HttpHeaders.AUTHORIZATION, bearer(requester)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("COMMON200"))
				.andExpect(jsonPath("$.result.length()").value(2))
				.andExpect(jsonPath("$.result[0].requestId").isNumber())
				.andExpect(jsonPath("$.result[0].projectId").isNumber())
				.andExpect(jsonPath("$.result[0].projectTitle").isString())
				.andExpect(jsonPath("$.result[*].status", containsInAnyOrder("APPROVED", "REJECTED")))
				.andExpect(jsonPath("$.result[0].decidedAt").value("2026-08-13T05:10:00Z"))
				.andReturn();

		JsonNode firstResponse = objectMapper.readTree(result.getResponse().getContentAsString())
				.path("result").get(0);
		assertThat(fieldNames(firstResponse)).containsExactly(
				"requestId", "projectId", "projectTitle", "status", "decidedAt");
	}

	@Test
	void 처리된_요청이_없으면_빈_배열을_반환한다() throws Exception {
		User requester = saveUser("요청자", "result-controller-empty@synq.com");

		mockMvc.perform(get("/projects/join-requests/me")
						.header(HttpHeaders.AUTHORIZATION, bearer(requester)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result").isArray())
				.andExpect(jsonPath("$.result").isEmpty());
	}

	@Test
	void JWT가_없거나_유효하지_않으면_401이다() throws Exception {
		mockMvc.perform(get("/projects/join-requests/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH401_1"));
		mockMvc.perform(get("/projects/join-requests/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH401_1"));
	}

	@Test
	void Swagger에_내_참여_요청_결과_API와_Bearer_인증이_문서화된다() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/projects/join-requests/me'].get.security[0].bearerAuth").exists())
				.andExpect(jsonPath("$.paths['/projects/join-requests/me'].get.responses['200']").exists())
				.andExpect(jsonPath("$.paths['/projects/join-requests/me'].get.responses['401']").exists())
				.andExpect(jsonPath("$.paths['/projects/join-requests/me'].get.responses['500']").exists())
				.andExpect(jsonPath("$.components.schemas.ProjectJoinRequestResultResponse.properties.requestId").exists())
				.andExpect(jsonPath("$.components.schemas.ProjectJoinRequestResultResponse.properties.projectId").exists())
				.andExpect(jsonPath("$.components.schemas.ProjectJoinRequestResultResponse.properties.projectTitle").exists())
				.andExpect(jsonPath("$.components.schemas.ProjectJoinRequestResultResponse.properties.status").exists())
				.andExpect(jsonPath("$.components.schemas.ProjectJoinRequestResultResponse.properties.decidedAt").exists());
	}

	private List<String> fieldNames(JsonNode node) {
		List<String> fieldNames = new ArrayList<>();
		node.fieldNames().forEachRemaining(fieldNames::add);
		return fieldNames;
	}

	private void setUpdatedAt(Long requestId, LocalDateTime updatedAt) {
		jdbcTemplate.update(
				"UPDATE project_join_request SET updated_at = ? WHERE id = ?",
				Timestamp.valueOf(updatedAt),
				requestId
		);
	}

	private Project saveProject(User owner, String title) {
		return projectRepository.save(Project.of(owner.getUserId(), title, null));
	}

	private ProjectParticipationRequest savePending(Project project, User requester) {
		return participationRequestRepository.save(ProjectParticipationRequest.pending(
				project.getId(),
				requester.getUserId(),
				ProjectJoinSettingSource.DEFAULT,
				Role.DEV_TECH,
				null
		));
	}

	private User saveUser(String name, String email) {
		return userRepository.save(User.ofLocal(name, email, "password-hash"));
	}
}
