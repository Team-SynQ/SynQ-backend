package com.synq.backend.domain.meeting.controller;

import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class MeetingEndControllerTest extends PostgresTestContainer {

	private static final long OWNER_ID = 100L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private MeetingRepository meetingRepository;

	private Long createInProgressMeeting() {
		Long projectId = projectRepository.save(Project.of(OWNER_ID, "테스트 프로젝트", "설명")).getId();
		return meetingRepository.save(Meeting.of(projectId, "회의")).getId();
	}

	@Test
	void 소유자가_종료하면_SUMMARIZING_으로_전환하고_종료시각을_기록한다() throws Exception {
		Long meetingId = createInProgressMeeting();

		mockMvc.perform(post("/meetings/{meetingId}/end", meetingId)
						.header("X-User-Id", OWNER_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.meetingId").value(meetingId))
				.andExpect(jsonPath("$.result.status").value("SUMMARIZING"))
				.andExpect(jsonPath("$.result.endedAt").isNotEmpty());
	}

	@Test
	void 소유자가_아니면_403과_도메인_에러코드를_반환한다() throws Exception {
		Long meetingId = createInProgressMeeting();

		mockMvc.perform(post("/meetings/{meetingId}/end", meetingId)
						.header("X-User-Id", OWNER_ID + 1))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("MEETING403_2"));
	}

	@Test
	void 존재하지_않는_회의면_404를_반환한다() throws Exception {
		mockMvc.perform(post("/meetings/{meetingId}/end", 999_999L)
						.header("X-User-Id", OWNER_ID))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("MEETING404_1"));
	}

	@Test
	void 이미_종료된_회의면_409를_반환한다() throws Exception {
		Long projectId = projectRepository.save(Project.of(OWNER_ID, "테스트 프로젝트", "설명")).getId();
		Meeting meeting = Meeting.of(projectId, "회의");
		meeting.end();
		Long meetingId = meetingRepository.save(meeting).getId();

		mockMvc.perform(post("/meetings/{meetingId}/end", meetingId)
						.header("X-User-Id", OWNER_ID))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("MEETING409_1"));
	}
}
