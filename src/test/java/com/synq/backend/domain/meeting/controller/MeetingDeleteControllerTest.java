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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class MeetingDeleteControllerTest extends PostgresTestContainer {

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

	private Long createEndedMeetingWithHost() {
		Meeting meeting = Meeting.of(1L, "회의");
		meeting.end();
		Long meetingId = meetingRepository.save(meeting).getId();
		meetingParticipantRepository.save(MeetingParticipant.of(meetingId, HOST_ID, ParticipantRole.HOST));
		return meetingId;
	}

	private Long createInProgressMeetingWithHost() {
		// project_id는 IN_PROGRESS 상태에서 유니크해야 하므로(uq_meeting_project_active), 매번 새 값을 쓴다.
		Long meetingId = meetingRepository.save(Meeting.of(System.nanoTime(), "회의")).getId();
		meetingParticipantRepository.save(MeetingParticipant.of(meetingId, HOST_ID, ParticipantRole.HOST));
		return meetingId;
	}

	@Test
	void 진행자가_종료된_회의를_삭제하면_200을_반환하고_참여자도_함께_삭제된다() throws Exception {
		Long meetingId = createEndedMeetingWithHost();

		mockMvc.perform(delete("/meetings/{meetingId}", meetingId)
						.header(HttpHeaders.AUTHORIZATION, bearer(HOST_ID)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true));

		assertThat(meetingRepository.existsById(meetingId)).isFalse();
		assertThat(meetingParticipantRepository.findByMeetingIdAndUserId(meetingId, HOST_ID)).isEmpty();
	}

	@Test
	void 진행_중인_회의를_삭제하려_하면_409를_반환한다() throws Exception {
		Long meetingId = createInProgressMeetingWithHost();

		mockMvc.perform(delete("/meetings/{meetingId}", meetingId)
						.header(HttpHeaders.AUTHORIZATION, bearer(HOST_ID)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("MEETING409_2"));

		assertThat(meetingRepository.existsById(meetingId)).isTrue();
	}

	@Test
	void 진행자가_아니면_403과_도메인_에러코드를_반환한다() throws Exception {
		Long meetingId = createEndedMeetingWithHost();

		mockMvc.perform(delete("/meetings/{meetingId}", meetingId)
						.header(HttpHeaders.AUTHORIZATION, bearer(HOST_ID + 1)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("MEETING403_6"));

		assertThat(meetingRepository.existsById(meetingId)).isTrue();
	}

	@Test
	void 존재하지_않는_회의면_404를_반환한다() throws Exception {
		mockMvc.perform(delete("/meetings/{meetingId}", 999_999L)
						.header(HttpHeaders.AUTHORIZATION, bearer(HOST_ID)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("MEETING404_1"));
	}

	@Test
	void 토큰_없이_호출하면_401을_반환한다() throws Exception {
		Long meetingId = createEndedMeetingWithHost();

		mockMvc.perform(delete("/meetings/{meetingId}", meetingId))
				.andExpect(status().isUnauthorized());
	}

	private String bearer(Long userId) {
		return "Bearer " + jwtProvider.createAccessToken(userId);
	}
}
