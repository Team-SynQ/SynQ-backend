package com.synq.backend.domain.project.controller;

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
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class ProjectLeaveControllerTest extends ProjectControllerTestSupport {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void MEMBER가_프로젝트에서_나가면_200과_빈_Body를_반환한다() throws Exception {
		User owner = saveUser("leave-controller-owner@synq.com");
		User member = saveUser("leave-controller-member@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		ProjectMember memberMembership = saveMember(project, member, ProjectMemberRole.MEMBER);

		mockMvc.perform(delete("/projects/{projectId}/members/me", project.getId())
						.header("Authorization", bearer(member)))
				.andExpect(status().isOk())
				.andExpect(content().string(""));

		assertThat(projectMemberRepository.findById(memberMembership.getId())).isEmpty();
	}

	@Test
	void JWT가_없거나_유효하지_않으면_401을_반환한다() throws Exception {
		mockMvc.perform(delete("/projects/{projectId}/members/me", 1L))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH401_1"));
		mockMvc.perform(delete("/projects/{projectId}/members/me", 1L)
						.header("Authorization", "Bearer invalid-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH401_1"));
	}

	@Test
	void 프로젝트_OWNER의_나가기_요청은_400이다() throws Exception {
		User owner = saveUser("leave-controller-owner-forbidden@synq.com");
		Project project = saveProject(owner);
		ProjectMember ownerMembership = saveMember(project, owner, ProjectMemberRole.OWNER);

		mockMvc.perform(delete("/projects/{projectId}/members/me", project.getId())
						.header("Authorization", bearer(owner)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("PROJECT400_2"));
		assertThat(projectMemberRepository.findById(ownerMembership.getId())).isPresent();
	}

	@Test
	void 프로젝트_외부_사용자와_이미_나간_사용자는_403이다() throws Exception {
		User owner = saveUser("leave-controller-non-member-owner@synq.com");
		User member = saveUser("leave-controller-already-member@synq.com");
		User outsider = saveUser("leave-controller-outsider@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(project, member, ProjectMemberRole.MEMBER);

		mockMvc.perform(delete("/projects/{projectId}/members/me", project.getId())
						.header("Authorization", bearer(outsider)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("PROJECT403_2"));

		mockMvc.perform(delete("/projects/{projectId}/members/me", project.getId())
						.header("Authorization", bearer(member)))
				.andExpect(status().isOk());
		mockMvc.perform(delete("/projects/{projectId}/members/me", project.getId())
						.header("Authorization", bearer(member)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("PROJECT403_2"));
	}

	@Test
	void 존재하지_않거나_삭제된_프로젝트는_404이다() throws Exception {
		User owner = saveUser("leave-controller-project-owner@synq.com");
		User member = saveUser("leave-controller-project-member@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		ProjectMember memberMembership = saveMember(project, member, ProjectMemberRole.MEMBER);
		project.softDelete();
		projectRepository.flush();

		mockMvc.perform(delete("/projects/{projectId}/members/me", Long.MAX_VALUE)
						.header("Authorization", bearer(member)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PROJECT404_3"));
		mockMvc.perform(delete("/projects/{projectId}/members/me", project.getId())
						.header("Authorization", bearer(member)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PROJECT404_3"));
		assertThat(projectMemberRepository.findById(memberMembership.getId())).isPresent();
	}

	@Test
	void X_User_Id를_MEMBER로_위조해도_JWT_사용자_기준으로_403이다() throws Exception {
		User owner = saveUser("leave-controller-forgery-owner@synq.com");
		User member = saveUser("leave-controller-forgery-member@synq.com");
		User outsider = saveUser("leave-controller-forgery-outsider@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		ProjectMember memberMembership = saveMember(project, member, ProjectMemberRole.MEMBER);

		mockMvc.perform(delete("/projects/{projectId}/members/me", project.getId())
						.header("Authorization", bearer(outsider))
						.header("X-User-Id", member.getUserId()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("PROJECT403_2"));
		assertThat(projectMemberRepository.findById(memberMembership.getId())).isPresent();
	}

	@Test
	void Swagger에_프로젝트_나가기_API와_Bearer_인증이_문서화된다() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/members/me'].delete").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/members/me'].delete.security[0].bearerAuth").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/members/me'].delete.responses['200']").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/members/me'].delete.responses['400']").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/members/me'].delete.responses['401']").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/members/me'].delete.responses['403']").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/members/me'].delete.responses['404']").exists());
	}

	private Project saveProject(User owner) {
		return projectRepository.save(Project.of(owner.getUserId(), "SynQ", null));
	}

	private ProjectMember saveMember(Project project, User user, ProjectMemberRole role) {
		return projectMemberRepository.save(ProjectMember.of(project.getId(), user.getUserId(), role));
	}

	private User saveUser(String email) {
		return userRepository.save(User.ofLocal("테스트", email, "password-hash"));
	}
}
