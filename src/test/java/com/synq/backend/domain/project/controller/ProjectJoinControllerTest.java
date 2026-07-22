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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class ProjectJoinControllerTest extends PostgresTestContainer {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void 신규_프로젝트_참여는_명세의_응답과_201을_반환한다() throws Exception {
		User owner = saveUser("controller-join-owner@synq.com");
		User participant = saveUser("controller-join-participant@synq.com");
		String inviteToken = UUID.randomUUID().toString();
		Project project = saveProjectWithOwner(owner, inviteToken);

		mockMvc.perform(post("/projects/join")
						.header("X-User-Id", participant.getUserId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"inviteToken\":\"%s\"}".formatted(inviteToken)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.result.projectId").value(project.getId()))
				.andExpect(jsonPath("$.result.title").value("SynQ"))
				.andExpect(jsonPath("$.result.description").value("회의 협업 프로젝트"))
				.andExpect(jsonPath("$.result.memberRole").value("MEMBER"))
				.andExpect(jsonPath("$.result.joinedAt").isNotEmpty())
				.andExpect(jsonPath("$.result.newlyJoined").doesNotExist());
	}

	@Test
	void 이미_참여한_프로젝트는_기존_정보와_200을_반환한다() throws Exception {
		User owner = saveUser("controller-duplicate-owner@synq.com");
		String inviteToken = UUID.randomUUID().toString();
		saveProjectWithOwner(owner, inviteToken);

		mockMvc.perform(post("/projects/join")
						.header("X-User-Id", owner.getUserId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"inviteToken\":\"%s\"}".formatted(inviteToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.memberRole").value("OWNER"))
				.andExpect(jsonPath("$.result.newlyJoined").doesNotExist());
	}

	@Test
	void 존재하지_않는_초대_토큰은_404를_반환한다() throws Exception {
		User participant = saveUser("controller-not-found@synq.com");

		mockMvc.perform(post("/projects/join")
						.header("X-User-Id", participant.getUserId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"inviteToken\":\"%s\"}".formatted(UUID.randomUUID())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PROJECT404_2"));
	}

	@Test
	void 만료된_초대_토큰은_410을_반환한다() throws Exception {
		User owner = saveUser("controller-expired-owner@synq.com");
		User participant = saveUser("controller-expired-participant@synq.com");
		String inviteToken = UUID.randomUUID().toString();
		Project project = Project.of(owner.getUserId(), "SynQ", null);
		project.updateInvitation(inviteToken, LocalDateTime.now().minusSeconds(1));
		projectRepository.save(project);
		projectMemberRepository.save(ProjectMember.of(project.getId(), owner.getUserId(), ProjectMemberRole.OWNER));

		mockMvc.perform(post("/projects/join")
						.header("X-User-Id", participant.getUserId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"inviteToken\":\"%s\"}".formatted(inviteToken)))
				.andExpect(status().isGone())
				.andExpect(jsonPath("$.code").value("PROJECT410_1"));
	}

	private Project saveProjectWithOwner(User owner, String inviteToken) {
		Project project = Project.of(owner.getUserId(), "SynQ", "회의 협업 프로젝트");
		project.updateInvitation(inviteToken, LocalDateTime.now().plusDays(7));
		projectRepository.save(project);
		projectMemberRepository.save(ProjectMember.of(project.getId(), owner.getUserId(), ProjectMemberRole.OWNER));
		return project;
	}

	private User saveUser(String email) {
		return userRepository.save(User.ofLocal("테스트", email, "password-hash"));
	}
}
