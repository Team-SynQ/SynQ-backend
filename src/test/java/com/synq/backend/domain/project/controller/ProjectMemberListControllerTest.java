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
class ProjectMemberListControllerTest extends ProjectControllerTestSupport {

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
	void 멤버_목록_응답의_필드_순서와_타입이_명세와_일치한다() throws Exception {
		User owner = saveUser("소유자", "member-list-controller-owner@synq.com");
		User member = saveUser("멤버", "member-list-controller-member@synq.com");
		Project project = saveProject(owner);
		ProjectMember ownerMembership = saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(project, member, ProjectMemberRole.MEMBER);

		MvcResult result = mockMvc.perform(get("/projects/{projectId}/members", project.getId())
						.header("Authorization", bearer(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.projectId").value(project.getId()))
				.andExpect(jsonPath("$.result.ownerId").value(owner.getUserId()))
				.andExpect(jsonPath("$.result.title").value("SynQ"))
				.andExpect(jsonPath("$.result.currentMemberCount").value(2))
				.andExpect(jsonPath("$.result.maxMemberCount").value(10))
				.andExpect(jsonPath("$.result.members").isArray())
				.andExpect(jsonPath("$.result.members[0].memberId").value(ownerMembership.getId()))
				.andExpect(jsonPath("$.result.members[0].userId").value(owner.getUserId()))
				.andExpect(jsonPath("$.result.members[0].nickname").value("소유자"))
				.andExpect(jsonPath("$.result.members[0].role").value("OWNER"))
				.andExpect(jsonPath("$.result.members[0].isMe").value(true))
				.andExpect(jsonPath("$.result.members[0].joinedAt").isString())
				.andReturn();

		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString()).path("result");
		assertThat(fieldNames(response)).containsExactly(
				"projectId",
				"ownerId",
				"title",
				"currentMemberCount",
				"maxMemberCount",
				"members"
		);
		assertThat(fieldNames(response.path("members").get(0))).containsExactly(
				"memberId",
				"userId",
				"nickname",
				"role",
				"isMe",
				"joinedAt"
		);
	}

	@Test
	void 프로젝트_외부_사용자는_403을_반환한다() throws Exception {
		User owner = saveUser("소유자", "member-list-controller-403-owner@synq.com");
		User outsider = saveUser("외부 사용자", "member-list-controller-403@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);

		mockMvc.perform(get("/projects/{projectId}/members", project.getId())
						.header("Authorization", bearer(outsider)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("PROJECT403_2"));
	}

	@Test
	void 존재하지_않는_프로젝트는_404를_반환한다() throws Exception {
		User user = saveUser("사용자", "member-list-controller-404@synq.com");

		mockMvc.perform(get("/projects/{projectId}/members", Long.MAX_VALUE)
						.header("Authorization", bearer(user)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PROJECT404_3"));
	}

	@Test
	void 삭제된_프로젝트는_404를_반환한다() throws Exception {
		User owner = saveUser("소유자", "member-list-controller-deleted@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		project.softDelete();

		mockMvc.perform(get("/projects/{projectId}/members", project.getId())
						.header("Authorization", bearer(owner)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PROJECT404_3"));
	}

	@Test
	void 인증_헤더가_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(get("/projects/{projectId}/members", 1L))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH401_1"));
	}

	@Test
	void Swagger에_프로젝트_멤버_목록_조회_API가_문서화된다() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/members'].get").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/members'].get.security[0].bearerAuth").exists());
	}

	private List<String> fieldNames(JsonNode node) {
		List<String> fieldNames = new ArrayList<>();
		node.fieldNames().forEachRemaining(fieldNames::add);
		return fieldNames;
	}

	private Project saveProject(User owner) {
		return projectRepository.save(Project.of(owner.getUserId(), "SynQ", "프로젝트 설명"));
	}

	private ProjectMember saveMember(Project project, User user, ProjectMemberRole role) {
		return projectMemberRepository.save(
				ProjectMember.of(project.getId(), user.getUserId(), role));
	}

	private User saveUser(String name, String email) {
		return userRepository.save(User.ofLocal(name, email, "password-hash"));
	}
}
