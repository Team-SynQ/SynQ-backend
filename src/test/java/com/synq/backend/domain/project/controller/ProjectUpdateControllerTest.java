package com.synq.backend.domain.project.controller;

import com.synq.backend.domain.project.entity.Project;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class ProjectUpdateControllerTest extends PostgresTestContainer {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void OWNER가_프로젝트를_수정하면_명세의_응답을_반환한다() throws Exception {
		User owner = saveUser("update-controller-owner@synq.com");
		Project project = saveProject(owner, "기존 제목", "기존 설명");

		mockMvc.perform(patch("/projects/{projectId}", project.getId())
						.header("X-User-Id", owner.getUserId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"수정 제목","description":"수정 설명"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.projectId").value(project.getId()))
				.andExpect(jsonPath("$.result.title").value("수정 제목"))
				.andExpect(jsonPath("$.result.description").value("수정 설명"))
				.andExpect(jsonPath("$.result.updatedAt").isNotEmpty())
				.andExpect(jsonPath("$.result.projectRole").doesNotExist());
	}

	@Test
	void title과_description이_모두_없으면_400을_반환한다() throws Exception {
		User owner = saveUser("update-controller-empty@synq.com");
		Project project = saveProject(owner, "기존 제목", null);

		mockMvc.perform(patch("/projects/{projectId}", project.getId())
						.header("X-User-Id", owner.getUserId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON400_1"));
	}

	@Test
	void OWNER가_아니면_403을_반환한다() throws Exception {
		User owner = saveUser("update-controller-forbidden-owner@synq.com");
		User member = saveUser("update-controller-forbidden-member@synq.com");
		Project project = saveProject(owner, "기존 제목", null);

		mockMvc.perform(patch("/projects/{projectId}", project.getId())
						.header("X-User-Id", member.getUserId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"수정 제목\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("PROJECT403_1"));
	}

	@Test
	void 존재하지_않는_프로젝트면_404를_반환한다() throws Exception {
		User owner = saveUser("update-controller-missing@synq.com");

		mockMvc.perform(patch("/projects/{projectId}", Long.MAX_VALUE)
						.header("X-User-Id", owner.getUserId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"수정 제목\"}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PROJECT404_3"));
	}

	@Test
	void 인증_헤더가_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(patch("/projects/{projectId}", 1L)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"수정 제목\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH401_1"));
	}

	@Test
	void title이_30자를_초과하면_400을_반환한다() throws Exception {
		User owner = saveUser("update-controller-title-validation@synq.com");
		Project project = saveProject(owner, "기존 제목", null);

		mockMvc.perform(patch("/projects/{projectId}", project.getId())
						.header("X-User-Id", owner.getUserId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"%s\"}".formatted("가".repeat(31))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON400_1"));
	}

	@Test
	void 공백으로만_이루어진_title이면_400을_반환한다() throws Exception {
		User owner = saveUser("update-controller-blank-title@synq.com");
		Project project = saveProject(owner, "기존 제목", null);

		mockMvc.perform(patch("/projects/{projectId}", project.getId())
						.header("X-User-Id", owner.getUserId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"   \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON400_1"));
	}

	@Test
	void description이_500자를_초과하면_400을_반환한다() throws Exception {
		User owner = saveUser("update-controller-description-validation@synq.com");
		Project project = saveProject(owner, "기존 제목", null);

		mockMvc.perform(patch("/projects/{projectId}", project.getId())
						.header("X-User-Id", owner.getUserId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"description\":\"%s\"}".formatted("가".repeat(501))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON400_1"));
	}

	private Project saveProject(User owner, String title, String description) {
		return projectRepository.save(Project.of(owner.getUserId(), title, description));
	}

	private User saveUser(String email) {
		return userRepository.save(User.ofLocal("테스트", email, "password-hash"));
	}
}
