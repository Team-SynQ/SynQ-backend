package com.synq.backend.domain.meeting.controller;

import com.synq.backend.domain.auth.jwt.AccessTokenBlacklistService;
import com.synq.backend.domain.auth.jwt.JwtProvider;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.entity.MeetingParticipant;
import com.synq.backend.domain.meeting.entity.ParticipantRole;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class MeetingResumeControllerTest extends PostgresTestContainer {

	private static final long HOST_ID = 100L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MeetingRepository meetingRepository;

	@Autowired
	private MeetingParticipantRepository meetingParticipantRepository;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private AccessTokenBlacklistService accessTokenBlacklistService;

	private Long createPausedMeetingWithHost() {
		Meeting meeting = Meeting.of(System.nanoTime(), "회의");
		meeting.pause();
		Long meetingId = meetingRepository.save(meeting).getId();
		meetingParticipantRepository.save(MeetingParticipant.of(meetingId, HOST_ID, ParticipantRole.HOST));
		return meetingId;
	}

	@Test
	void 진행자가_재개하면_paused가_false가_되고_activeSeconds를_반환한다() throws Exception {
		Long meetingId = createPausedMeetingWithHost();

		mockMvc.perform(post("/meetings/{meetingId}/resume", meetingId)
						.header(HttpHeaders.AUTHORIZATION, bearer(HOST_ID)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.meetingId").value(meetingId))
				.andExpect(jsonPath("$.result.paused").value(false))
				.andExpect(jsonPath("$.result.activeSeconds").isNumber());
	}

	@Test
	void 진행자가_아니면_403과_도메인_에러코드를_반환한다() throws Exception {
		Long meetingId = createPausedMeetingWithHost();

		mockMvc.perform(post("/meetings/{meetingId}/resume", meetingId)
						.header(HttpHeaders.AUTHORIZATION, bearer(HOST_ID + 1)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("MEETING403_10"));
	}

	@Test
	void 존재하지_않는_회의면_404를_반환한다() throws Exception {
		mockMvc.perform(post("/meetings/{meetingId}/resume", 999_999L)
						.header(HttpHeaders.AUTHORIZATION, bearer(HOST_ID)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("MEETING404_1"));
	}

	@Test
	void 일시정지_상태가_아니면_409를_반환한다() throws Exception {
		Meeting meeting = Meeting.of(1L, "회의");
		Long meetingId = meetingRepository.save(meeting).getId();
		meetingParticipantRepository.save(MeetingParticipant.of(meetingId, HOST_ID, ParticipantRole.HOST));

		mockMvc.perform(post("/meetings/{meetingId}/resume", meetingId)
						.header(HttpHeaders.AUTHORIZATION, bearer(HOST_ID)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("MEETING409_5"));
	}

	@Test
	void 이미_종료된_회의면_409를_반환한다() throws Exception {
		Meeting meeting = Meeting.of(1L, "회의");
		meeting.pause();
		meeting.end();
		Long meetingId = meetingRepository.save(meeting).getId();
		meetingParticipantRepository.save(MeetingParticipant.of(meetingId, HOST_ID, ParticipantRole.HOST));

		mockMvc.perform(post("/meetings/{meetingId}/resume", meetingId)
						.header(HttpHeaders.AUTHORIZATION, bearer(HOST_ID)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("MEETING409_1"));
	}

	@Test
	void 토큰_없이_호출하면_401을_반환한다() throws Exception {
		Long meetingId = createPausedMeetingWithHost();

		mockMvc.perform(post("/meetings/{meetingId}/resume", meetingId))
				.andExpect(status().isUnauthorized());
	}

	private String bearer(Long userId) {
		return "Bearer " + jwtProvider.createAccessToken(userId);
	}
}
