package com.synq.backend.domain.project.controller;

import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class ProjectListControllerTest extends PostgresTestContainer {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;

	@Autowired
	private MeetingRepository meetingRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void 참여_중인_프로젝트_목록과_명세의_응답_필드를_반환한다() throws Exception {
		User user = saveUser("project-list-controller@synq.com");
		Project project = projectRepository.save(
				Project.of(user.getUserId(), "SynQ", "AI 회의 협업 프로젝트"));
		projectMemberRepository.save(
				ProjectMember.of(project.getId(), user.getUserId(), ProjectMemberRole.OWNER));
		meetingRepository.save(Meeting.of(project.getId(), "1차 기획회의"));

		mockMvc.perform(get("/projects")
						.header("X-User-Id", user.getUserId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.result[0].projectId").value(project.getId()))
				.andExpect(jsonPath("$.result[0].title").value("SynQ"))
				.andExpect(jsonPath("$.result[0].description").value("AI 회의 협업 프로젝트"))
				.andExpect(jsonPath("$.result[0].recentMeetingTitle").value("1차 기획회의"))
				.andExpect(jsonPath("$.result[0].updatedAt").isNotEmpty());
	}

	@Test
	void 참여한_프로젝트가_없으면_빈_목록을_반환한다() throws Exception {
		User user = saveUser("empty-project-list-controller@synq.com");

		mockMvc.perform(get("/projects")
						.header("X-User-Id", user.getUserId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result").isArray())
				.andExpect(jsonPath("$.result").isEmpty());
	}

	@Test
	void 인증_헤더가_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(get("/projects"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH401_1"));
	}

	private User saveUser(String email) {
		return userRepository.save(User.ofLocal("테스트", email, "password-hash"));
	}
}
