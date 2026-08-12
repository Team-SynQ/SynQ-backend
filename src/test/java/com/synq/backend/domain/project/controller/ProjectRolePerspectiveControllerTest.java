package com.synq.backend.domain.project.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.project.entity.ProjectMemberRole;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.user.entity.Perspective;
import com.synq.backend.domain.user.entity.Role;
import com.synq.backend.domain.user.entity.RoleProfile;
import com.synq.backend.domain.user.entity.RoleProfilePerspective;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.RoleProfilePerspectiveRepository;
import com.synq.backend.domain.user.repository.RoleProfileRepository;
import com.synq.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class ProjectRolePerspectiveControllerTest extends ProjectControllerTestSupport {

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

	@Autowired
	private RoleProfileRepository roleProfileRepository;

	@Autowired
	private RoleProfilePerspectiveRepository roleProfilePerspectiveRepository;

	@Test
	void 기본_역할_관점이_적용된_조회_응답은_명세의_필드_순서를_따른다() throws Exception {
		User user = saveUser("role-controller-get@synq.com");
		Project project = saveProjectWithMember(user);
		saveDefaultProfile(user, Role.DEV_TECH, "백엔드 개발자",
				List.of(Perspective.SCHEDULE, Perspective.TECH_RISK));

		MvcResult result = mockMvc.perform(get("/projects/{projectId}/role-perspective", project.getId())
						.header("Authorization", bearer(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.useDefault").value(true))
				.andExpect(jsonPath("$.result.roleCategory").value("DEV_TECH"))
				.andExpect(jsonPath("$.result.detailRole").value("백엔드 개발자"))
				.andExpect(jsonPath("$.result.perspectives[0]").value("SCHEDULE"))
				.andExpect(jsonPath("$.result.perspectives[1]").value("TECH_RISK"))
				.andReturn();

		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString()).path("result");
		List<String> fieldNames = new ArrayList<>();
		response.fieldNames().forEachRemaining(fieldNames::add);
		assertThat(fieldNames).containsExactly(
				"useDefault", "roleCategory", "detailRole", "perspectives");
	}

	@Test
	void 프로젝트별_역할_관점을_수정하면_명세의_응답을_반환한다() throws Exception {
		User user = saveUser("role-controller-put@synq.com");
		Project project = saveProjectWithMember(user);

		MvcResult result = mockMvc.perform(put("/projects/{projectId}/role-perspective", project.getId())
						.header("Authorization", bearer(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "useDefault": false,
								  "roleCategory": "DEV_TECH",
								  "detailRole": "백엔드 개발자",
								  "perspectives": ["TECH_RISK", "ACTION_ITEM"]
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.projectId").value(project.getId()))
				.andExpect(jsonPath("$.result.useDefault").value(false))
				.andExpect(jsonPath("$.result.roleCategory").value("DEV_TECH"))
				.andExpect(jsonPath("$.result.detailRole").value("백엔드 개발자"))
				.andExpect(jsonPath("$.result.perspectives[0]").value("TECH_RISK"))
				.andExpect(jsonPath("$.result.perspectives[1]").value("ACTION_ITEM"))
				.andExpect(jsonPath("$.result.updatedAt").isNotEmpty())
				.andReturn();

		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString()).path("result");
		List<String> fieldNames = new ArrayList<>();
		response.fieldNames().forEachRemaining(fieldNames::add);
		assertThat(fieldNames).containsExactly(
				"projectId", "useDefault", "roleCategory", "detailRole", "perspectives", "updatedAt");
	}

	@Test
	void 기본_설정_사용은_useDefault만_전달해도_성공한다() throws Exception {
		User user = saveUser("role-controller-default@synq.com");
		Project project = saveProjectWithMember(user);
		saveDefaultProfile(user, Role.PLANNING_OPERATION, "PM", List.of(Perspective.SCOPE));

		mockMvc.perform(put("/projects/{projectId}/role-perspective", project.getId())
						.header("Authorization", bearer(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"useDefault\":true}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.useDefault").value(true))
				.andExpect(jsonPath("$.result.roleCategory").value("PLANNING_OPERATION"))
				.andExpect(jsonPath("$.result.detailRole").value("PM"))
				.andExpect(jsonPath("$.result.perspectives[0]").value("SCOPE"));
	}

	@Test
	void 역할_관점_입력값이_올바르지_않으면_400을_반환한다() throws Exception {
		User user = saveUser("role-controller-invalid@synq.com");
		Project project = saveProjectWithMember(user);

		mockMvc.perform(put("/projects/{projectId}/role-perspective", project.getId())
						.header("Authorization", bearer(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"useDefault":false,"roleCategory":"ETC","perspectives":[]}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("PROJECT400_4"));

		mockMvc.perform(put("/projects/{projectId}/role-perspective", project.getId())
						.header("Authorization", bearer(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"roleCategory\":\"DEV_TECH\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void 인증_토큰이_없거나_유효하지_않으면_401을_반환한다() throws Exception {
		mockMvc.perform(get("/projects/1/role-perspective"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH401_1"));

		mockMvc.perform(put("/projects/1/role-perspective")
						.header("Authorization", "Bearer invalid-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"useDefault\":true}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH401_1"));
	}

	@Test
	void 프로젝트_외부_사용자는_위조한_X_User_Id를_보내도_403을_반환한다() throws Exception {
		User owner = saveUser("role-controller-owner@synq.com");
		User outsider = saveUser("role-controller-outsider@synq.com");
		Project project = saveProjectWithMember(owner);

		mockMvc.perform(put("/projects/{projectId}/role-perspective", project.getId())
						.header("Authorization", bearer(outsider))
						.header("X-User-Id", owner.getUserId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"useDefault\":true}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("PROJECT403_2"));
	}

	@Test
	void 존재하지_않거나_삭제된_프로젝트는_404를_반환한다() throws Exception {
		User user = saveUser("role-controller-not-found@synq.com");
		Project deletedProject = saveProjectWithMember(user);
		deletedProject.softDelete();
		projectRepository.flush();

		mockMvc.perform(get("/projects/{projectId}/role-perspective", Long.MAX_VALUE)
						.header("Authorization", bearer(user)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PROJECT404_3"));
		mockMvc.perform(get("/projects/{projectId}/role-perspective", deletedProject.getId())
						.header("Authorization", bearer(user)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PROJECT404_3"));
	}

	@Test
	void Swagger에_GET_PUT_경로와_Bearer_인증이_문서화된다() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/role-perspective'].get.security[0].bearerAuth").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/role-perspective'].put.security[0].bearerAuth").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/role-perspective'].get.responses['500']").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/role-perspective'].put.responses['400']").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/role-perspective'].put.responses['500']").exists());
	}

	private Project saveProjectWithMember(User user) {
		Project project = projectRepository.save(Project.of(user.getUserId(), "SynQ", null));
		projectMemberRepository.save(ProjectMember.of(
				project.getId(), user.getUserId(), ProjectMemberRole.OWNER));
		return project;
	}

	private void saveDefaultProfile(
			User user,
			Role role,
			String detailRole,
			List<Perspective> perspectives
	) {
		RoleProfile profile = roleProfileRepository.save(
				RoleProfile.of(user.getUserId(), role, detailRole, true));
		roleProfilePerspectiveRepository.saveAll(perspectives.stream()
				.map(perspective -> RoleProfilePerspective.of(profile.getId(), perspective))
				.toList());
	}

	private User saveUser(String email) {
		return userRepository.save(User.ofLocal("테스트", email, "password-hash"));
	}
}
