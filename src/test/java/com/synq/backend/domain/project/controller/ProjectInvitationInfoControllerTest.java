package com.synq.backend.domain.project.controller;

import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.project.entity.ProjectMemberRole;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class ProjectInvitationInfoControllerTest extends PostgresTestContainer {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void 초대_정보를_조회하면_명세의_응답과_200을_반환한다() throws Exception {
		User owner = saveUser("controller-info-owner@synq.com");
		User member = saveUser("controller-info-member@synq.com");
		String inviteToken = UUID.randomUUID().toString();
		Project project = saveProject(owner, inviteToken, LocalDateTime.now().plusDays(7));
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(project, member, ProjectMemberRole.MEMBER);

		mockMvc.perform(get("/projects/invitations/{inviteToken}", inviteToken)
						.header("X-User-Id", member.getUserId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.projectId").value(project.getId()))
				.andExpect(jsonPath("$.result.title").value("SynQ"))
				.andExpect(jsonPath("$.result.description").value("AI 회의 협업 프로젝트"))
				.andExpect(jsonPath("$.result.currentMemberCount").value(2))
				.andExpect(jsonPath("$.result.maxMemberCount").value(10))
				.andExpect(jsonPath("$.result.alreadyJoined").value(true))
				.andExpect(jsonPath("$.result.expiresAt").isNotEmpty());
	}

	@Test
	void 비로그인_사용자도_초대_정보를_조회할_수_있다() throws Exception {
		User owner = saveUser("controller-anonymous-owner@synq.com");
		String inviteToken = UUID.randomUUID().toString();
		Project project = saveProject(owner, inviteToken, LocalDateTime.now().plusDays(7));
		saveMember(project, owner, ProjectMemberRole.OWNER);

		mockMvc.perform(get("/projects/invitations/{inviteToken}", inviteToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.alreadyJoined").value(false));
	}

	@Test
	void 유효하지_않은_토큰이면_404를_반환한다() throws Exception {
		mockMvc.perform(get("/projects/invitations/{inviteToken}", UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PROJECT404_2"));
	}

	@Test
	void 만료된_토큰이면_410을_반환한다() throws Exception {
		User owner = saveUser("controller-expired-info-owner@synq.com");
		String inviteToken = UUID.randomUUID().toString();
		saveProject(owner, inviteToken, LocalDateTime.now().minusSeconds(1));

		mockMvc.perform(get("/projects/invitations/{inviteToken}", inviteToken))
				.andExpect(status().isGone())
				.andExpect(jsonPath("$.code").value("PROJECT410_1"));
	}

	@Test
	void Swagger에_프로젝트_초대_정보_조회_API가_문서화된다() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/projects/invitations/{inviteToken}'].get").exists());
	}

	private Project saveProject(User owner, String inviteToken, LocalDateTime expiresAt) {
		Project project = Project.of(owner.getUserId(), "SynQ", "AI 회의 협업 프로젝트");
		project.updateInvitation(inviteToken, expiresAt);
		return projectRepository.save(project);
	}

	private void saveMember(Project project, User user, ProjectMemberRole role) {
		projectMemberRepository.save(ProjectMember.of(project.getId(), user.getUserId(), role));
	}

	private User saveUser(String email) {
		return userRepository.save(User.ofLocal("테스트", email, "password-hash"));
	}
}
