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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class ProjectDeleteControllerTest extends PostgresTestContainer {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void OWNER가_프로젝트를_삭제하면_204와_빈_Body를_반환한다() throws Exception {
		User owner = saveUser("delete-controller-owner@synq.com");
		Project project = saveProject(owner);

		mockMvc.perform(delete("/projects/{projectId}", project.getId())
						.header("X-User-Id", owner.getUserId()))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));
	}

	@Test
	void OWNER가_아니면_403을_반환한다() throws Exception {
		User owner = saveUser("delete-controller-forbidden-owner@synq.com");
		User member = saveUser("delete-controller-forbidden-member@synq.com");
		Project project = saveProject(owner);

		mockMvc.perform(delete("/projects/{projectId}", project.getId())
						.header("X-User-Id", member.getUserId()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("PROJECT403_1"));
	}

	@Test
	void 존재하지_않는_프로젝트면_404를_반환한다() throws Exception {
		User owner = saveUser("delete-controller-missing@synq.com");

		mockMvc.perform(delete("/projects/{projectId}", Long.MAX_VALUE)
						.header("X-User-Id", owner.getUserId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PROJECT404_3"));
	}

	@Test
	void 인증_헤더가_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(delete("/projects/{projectId}", 1L))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH401_1"));
	}

	private Project saveProject(User owner) {
		return projectRepository.save(Project.of(owner.getUserId(), "SynQ", null));
	}

	private User saveUser(String email) {
		return userRepository.save(User.ofLocal("테스트", email, "password-hash"));
	}
}
