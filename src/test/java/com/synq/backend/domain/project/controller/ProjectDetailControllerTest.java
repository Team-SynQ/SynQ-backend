package com.synq.backend.domain.project.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.project.entity.ProjectMemberRole;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class ProjectDetailControllerTest extends ProjectControllerTestSupport {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void 프로젝트_상세_응답의_필드_순서와_타입이_명세와_일치한다() throws Exception {
		User owner = saveUser("detail-controller-owner@synq.com");
		Project project = saveProject(owner, "SynQ", "회의 협업 프로젝트");
		saveMember(project, owner, ProjectMemberRole.OWNER);

		MvcResult result = mockMvc.perform(get("/projects/{projectId}", project.getId())
						.header("Authorization", bearer(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.projectId").isNumber())
				.andExpect(jsonPath("$.result.ownerId").isNumber())
				.andExpect(jsonPath("$.result.title").isString())
				.andExpect(jsonPath("$.result.description").isString())
				.andExpect(jsonPath("$.result.activeMeetingId").value(org.hamcrest.Matchers.nullValue()))
				.andExpect(jsonPath("$.result.activeMeetingStartedAt").value(org.hamcrest.Matchers.nullValue()))
				.andExpect(jsonPath("$.result.createdAt").isString())
				.andExpect(jsonPath("$.result.updatedAt").isString())
				.andExpect(jsonPath("$.result.projectRole").doesNotExist())
				.andReturn();

		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString()).path("result");
		List<String> fieldNames = new ArrayList<>();
		response.fieldNames().forEachRemaining(fieldNames::add);
		assertThat(fieldNames).containsExactly(
				"projectId",
				"ownerId",
				"title",
				"description",
				"activeMeetingId",
				"activeMeetingStartedAt",
				"createdAt",
				"updatedAt"
		);
	}

	@Test
	void MEMBER가_description이_없는_프로젝트를_조회해도_200을_반환한다() throws Exception {
		User owner = saveUser("detail-null-owner@synq.com");
		User member = saveUser("detail-null-member@synq.com");
		Project project = saveProject(owner, "SynQ", null);
		saveMember(project, member, ProjectMemberRole.MEMBER);

		mockMvc.perform(get("/projects/{projectId}", project.getId())
						.header("Authorization", bearer(member)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.description").value(
						org.hamcrest.Matchers.nullValue()));
	}

	@Test
	void 프로젝트_외부_사용자는_403을_반환한다() throws Exception {
		User owner = saveUser("detail-403-owner@synq.com");
		User outsider = saveUser("detail-403-outsider@synq.com");
		Project project = saveProject(owner, "SynQ", null);

		mockMvc.perform(get("/projects/{projectId}", project.getId())
						.header("Authorization", bearer(outsider)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("PROJECT403_2"));
	}

	@Test
	void 존재하지_않는_프로젝트는_404를_반환한다() throws Exception {
		User user = saveUser("detail-404@synq.com");

		mockMvc.perform(get("/projects/{projectId}", Long.MAX_VALUE)
						.header("Authorization", bearer(user)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PROJECT404_3"));
	}

	@Test
	void 인증_헤더가_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(get("/projects/{projectId}", 1L))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH401_1"));
	}

	private Project saveProject(User owner, String title, String description) {
		return projectRepository.save(Project.of(owner.getUserId(), title, description));
	}

	private void saveMember(Project project, User user, ProjectMemberRole role) {
		projectMemberRepository.save(ProjectMember.of(project.getId(), user.getUserId(), role));
	}

	private User saveUser(String email) {
		return userRepository.save(User.ofLocal("테스트", email, "password-hash"));
	}
}
