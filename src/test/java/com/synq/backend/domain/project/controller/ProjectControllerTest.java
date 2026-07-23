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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class ProjectControllerTest extends PostgresTestContainer {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void 프로젝트를_생성하고_명세의_응답을_반환한다() throws Exception {
		User owner = saveUser("controller-owner@synq.com");

		mockMvc.perform(post("/projects")
						.header("X-User-Id", owner.getUserId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"SynQ","description":"회의 협업 프로젝트"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.result.projectId").isNumber())
				.andExpect(jsonPath("$.result.ownerId").value(owner.getUserId()))
				.andExpect(jsonPath("$.result.title").value("SynQ"))
				.andExpect(jsonPath("$.result.description").value("회의 협업 프로젝트"))
				.andExpect(jsonPath("$.result.createdAt").isNotEmpty())
				.andExpect(jsonPath("$.result.inviteToken").doesNotExist());

		Project project = projectRepository.findAll().stream()
				.filter(saved -> saved.getOwnerId().equals(owner.getUserId()))
				.findFirst().orElseThrow();
		ProjectMember member = projectMemberRepository
				.findByProjectIdAndUserId(project.getId(), owner.getUserId()).orElseThrow();
		assertThat(member.getRole()).isEqualTo(ProjectMemberRole.OWNER);
	}

	@Test
	void 프로젝트명_길이가_30자를_초과하면_400을_반환한다() throws Exception {
		User owner = saveUser("controller-validation@synq.com");

		mockMvc.perform(post("/projects")
						.header("X-User-Id", owner.getUserId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"%s\",\"description\":null}".formatted("가".repeat(31))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON400_1"));
	}

	private User saveUser(String email) {
		return userRepository.save(User.ofLocal("테스트", email, "password-hash"));
	}
}
