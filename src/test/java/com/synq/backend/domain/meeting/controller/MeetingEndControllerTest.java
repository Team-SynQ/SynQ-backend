package com.synq.backend.domain.meeting.controller;

import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.entity.MeetingParticipant;
import com.synq.backend.domain.meeting.entity.ParticipantRole;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
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

	private static final long HOST_ID = 100L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MeetingRepository meetingRepository;

	@Autowired
	private MeetingParticipantRepository meetingParticipantRepository;

	// 회의 생성 시 생성자를 HOST 참여자로 저장하는 것과 동일한 상태를 만든다.
	private Long createInProgressMeetingWithHost() {
		Long meetingId = meetingRepository.save(Meeting.of(1L, "회의")).getId();
		meetingParticipantRepository.save(MeetingParticipant.of(meetingId, HOST_ID, ParticipantRole.HOST));
		return meetingId;
	}

	@Test
	void 진행자가_종료하면_SUMMARIZING_으로_전환하고_종료시각을_기록한다() throws Exception {
		Long meetingId = createInProgressMeetingWithHost();

		mockMvc.perform(post("/meetings/{meetingId}/end", meetingId)
						.header("X-User-Id", HOST_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.meetingId").value(meetingId))
				.andExpect(jsonPath("$.result.status").value("SUMMARIZING"))
				.andExpect(jsonPath("$.result.endedAt").isNotEmpty());
	}

	@Test
	void 진행자가_아니면_403과_도메인_에러코드를_반환한다() throws Exception {
		Long meetingId = createInProgressMeetingWithHost();

		mockMvc.perform(post("/meetings/{meetingId}/end", meetingId)
						.header("X-User-Id", HOST_ID + 1))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("MEETING403_2"));
	}

	@Test
	void 존재하지_않는_회의면_404를_반환한다() throws Exception {
		mockMvc.perform(post("/meetings/{meetingId}/end", 999_999L)
						.header("X-User-Id", HOST_ID))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("MEETING404_1"));
	}

	@Test
	void 이미_종료된_회의면_409를_반환한다() throws Exception {
		Meeting meeting = Meeting.of(1L, "회의");
		meeting.end();
		Long meetingId = meetingRepository.save(meeting).getId();
		meetingParticipantRepository.save(MeetingParticipant.of(meetingId, HOST_ID, ParticipantRole.HOST));

		mockMvc.perform(post("/meetings/{meetingId}/end", meetingId)
						.header("X-User-Id", HOST_ID))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("MEETING409_1"));
	}
}
