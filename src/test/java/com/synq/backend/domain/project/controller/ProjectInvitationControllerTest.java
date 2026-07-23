package com.synq.backend.domain.project.controller;

import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class ProjectInvitationControllerTest extends PostgresTestContainer {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void 프로젝트_소유자가_초대_링크를_생성하면_200을_반환한다() throws Exception {
		User owner = saveUser("controller-invitation-owner@synq.com");
		Project project = projectRepository.save(Project.of(owner.getUserId(), "SynQ", null));

		mockMvc.perform(post("/projects/{projectId}/invitation", project.getId())
						.header("X-User-Id", owner.getUserId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.result.inviteUrl").value(
						org.hamcrest.Matchers.startsWith("https://synq.app/invite/")))
				.andExpect(jsonPath("$.result.expiresAt").isNotEmpty());
	}

	@Test
	void 인증_헤더가_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(post("/projects/{projectId}/invitation", 1L))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH401_1"));
	}

	@Test
	void 존재하지_않는_프로젝트면_404를_반환한다() throws Exception {
		User owner = saveUser("controller-missing-project@synq.com");

		mockMvc.perform(post("/projects/{projectId}/invitation", Long.MAX_VALUE)
						.header("X-User-Id", owner.getUserId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PROJECT404_3"));
	}

	@Test
	void 프로젝트_소유자가_아니면_403을_반환한다() throws Exception {
		User owner = saveUser("controller-permission-owner@synq.com");
		User member = saveUser("controller-permission-member@synq.com");
		Project project = projectRepository.save(Project.of(owner.getUserId(), "SynQ", null));

		mockMvc.perform(post("/projects/{projectId}/invitation", project.getId())
						.header("X-User-Id", member.getUserId()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("PROJECT403_1"));
	}

	private User saveUser(String email) {
		return userRepository.save(User.ofLocal("테스트", email, "password-hash"));
	}
}
