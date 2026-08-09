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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class MeetingJoinControllerTest extends PostgresTestContainer {

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

	private Long createInProgressMeetingWithHost() {
		// project_id는 IN_PROGRESS 상태에서 유니크해야 하므로(uq_meeting_project_active), 매번 새 값을 쓴다.
		Long meetingId = meetingRepository.save(Meeting.of(System.nanoTime(), "회의")).getId();
		meetingParticipantRepository.save(MeetingParticipant.of(meetingId, HOST_ID, ParticipantRole.HOST));
		return meetingId;
	}

	@Test
	void 프로젝트_멤버가_참여하면_MEMBER로_등록된다() throws Exception {
		Long meetingId = createInProgressMeetingWithHost();

		mockMvc.perform(post("/meetings/{meetingId}/join", meetingId)
						.header(HttpHeaders.AUTHORIZATION, bearer(MEMBER_ID)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.meetingId").value(meetingId))
				.andExpect(jsonPath("$.result.role").value("MEMBER"))
				.andExpect(jsonPath("$.result.joinedAt").isNotEmpty())
				.andExpect(jsonPath("$.result.wsUrl").isNotEmpty());

		List<MeetingParticipant> members =
				meetingParticipantRepository.findByMeetingIdAndRole(meetingId, ParticipantRole.MEMBER);
		assertThat(members).hasSize(1);
		assertThat(members.get(0).getUserId()).isEqualTo(MEMBER_ID);
	}

	@Test
	void 이미_참여_중이면_새_row를_만들지_않고_기존_정보를_그대로_반환한다() throws Exception {
		Long meetingId = createInProgressMeetingWithHost();

		mockMvc.perform(post("/meetings/{meetingId}/join", meetingId)
						.header(HttpHeaders.AUTHORIZATION, bearer(HOST_ID)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.role").value("HOST"));

		List<MeetingParticipant> hosts =
				meetingParticipantRepository.findByMeetingIdAndRole(meetingId, ParticipantRole.HOST);
		assertThat(hosts).hasSize(1);
	}

	@Test
	void 존재하지_않는_회의면_404를_반환한다() throws Exception {
		mockMvc.perform(post("/meetings/{meetingId}/join", 999_999L)
						.header(HttpHeaders.AUTHORIZATION, bearer(MEMBER_ID)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("MEETING404_1"));
	}

	@Test
	void 이미_종료된_회의면_409를_반환한다() throws Exception {
		Meeting meeting = Meeting.of(1L, "회의");
		meeting.end();
		Long meetingId = meetingRepository.save(meeting).getId();
		meetingParticipantRepository.save(MeetingParticipant.of(meetingId, HOST_ID, ParticipantRole.HOST));

		mockMvc.perform(post("/meetings/{meetingId}/join", meetingId)
						.header(HttpHeaders.AUTHORIZATION, bearer(MEMBER_ID)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("MEETING409_1"));
	}

	@Test
	void 이미_참여했던_사람도_종료된_회의엔_다시_들어올_수_없다() throws Exception {
		Meeting meeting = Meeting.of(1L, "회의");
		meeting.end();
		Long meetingId = meetingRepository.save(meeting).getId();
		meetingParticipantRepository.save(MeetingParticipant.of(meetingId, HOST_ID, ParticipantRole.HOST));

		mockMvc.perform(post("/meetings/{meetingId}/join", meetingId)
						.header(HttpHeaders.AUTHORIZATION, bearer(HOST_ID)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("MEETING409_1"));
	}

	@Test
	void 토큰_없이_호출하면_401을_반환한다() throws Exception {
		Long meetingId = createInProgressMeetingWithHost();

		mockMvc.perform(post("/meetings/{meetingId}/join", meetingId))
				.andExpect(status().isUnauthorized());
	}

	private String bearer(Long userId) {
		return "Bearer " + jwtProvider.createAccessToken(userId);
	}
}
