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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class MeetingLeaveControllerTest extends PostgresTestContainer {

	private static final long HOST_ID = 100L;
	private static final long MEMBER_ID = 200L;

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

	private Long createInProgressMeetingWithHostAndMember() {
		// project_id는 IN_PROGRESS 상태에서 유니크해야 하므로(uq_meeting_project_active), 매번 새 값을 쓴다.
		Long meetingId = meetingRepository.save(Meeting.of(System.nanoTime(), "회의")).getId();
		meetingParticipantRepository.save(MeetingParticipant.of(meetingId, HOST_ID, ParticipantRole.HOST));
		meetingParticipantRepository.save(MeetingParticipant.of(meetingId, MEMBER_ID, ParticipantRole.MEMBER));
		return meetingId;
	}

	@Test
	void 활성_참여자가_나가면_200을_반환하고_leftAt이_세팅된다() throws Exception {
		Long meetingId = createInProgressMeetingWithHostAndMember();

		mockMvc.perform(post("/meetings/{meetingId}/leave", meetingId)
						.header(HttpHeaders.AUTHORIZATION, bearer(MEMBER_ID)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true));

		assertThat(meetingParticipantRepository.existsByMeetingIdAndUserIdAndLeftAtIsNull(meetingId, MEMBER_ID))
				.isFalse();
	}

	@Test
	void 나간_뒤_다시_참여하면_새_MEMBER_row가_생긴다() throws Exception {
		Long meetingId = createInProgressMeetingWithHostAndMember();

		mockMvc.perform(post("/meetings/{meetingId}/leave", meetingId)
						.header(HttpHeaders.AUTHORIZATION, bearer(MEMBER_ID)))
				.andExpect(status().isOk());

		mockMvc.perform(post("/meetings/{meetingId}/join", meetingId)
						.header(HttpHeaders.AUTHORIZATION, bearer(MEMBER_ID)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.role").value("MEMBER"));

		assertThat(meetingParticipantRepository.findByMeetingIdAndUserId(meetingId, MEMBER_ID)).hasSize(2);
	}

	@Test
	void 호스트가_나가려_하면_403을_반환한다() throws Exception {
		Long meetingId = createInProgressMeetingWithHostAndMember();

		mockMvc.perform(post("/meetings/{meetingId}/leave", meetingId)
						.header(HttpHeaders.AUTHORIZATION, bearer(HOST_ID)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("MEETING403_7"));

		assertThat(meetingParticipantRepository.existsByMeetingIdAndUserIdAndLeftAtIsNull(meetingId, HOST_ID))
				.isTrue();
	}

	@Test
	void 참여자가_아니면_403을_반환한다() throws Exception {
		Long meetingId = createInProgressMeetingWithHostAndMember();

		mockMvc.perform(post("/meetings/{meetingId}/leave", meetingId)
						.header(HttpHeaders.AUTHORIZATION, bearer(999L)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("MEETING403_4"));
	}

	@Test
	void 존재하지_않는_회의면_404를_반환한다() throws Exception {
		mockMvc.perform(post("/meetings/{meetingId}/leave", 999_999L)
						.header(HttpHeaders.AUTHORIZATION, bearer(MEMBER_ID)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("MEETING404_1"));
	}

	@Test
	void 토큰_없이_호출하면_401을_반환한다() throws Exception {
		Long meetingId = createInProgressMeetingWithHostAndMember();

		mockMvc.perform(post("/meetings/{meetingId}/leave", meetingId))
				.andExpect(status().isUnauthorized());
	}

	private String bearer(Long userId) {
		return "Bearer " + jwtProvider.createAccessToken(userId);
	}
}
