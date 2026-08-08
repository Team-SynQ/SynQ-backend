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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class MeetingTitleControllerTest extends PostgresTestContainer {

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

	private Long createMeetingWithHost() {
		Long meetingId = meetingRepository.save(Meeting.of(1L, "회의")).getId();
		meetingParticipantRepository.save(MeetingParticipant.of(meetingId, HOST_ID, ParticipantRole.HOST));
		return meetingId;
	}

	@Test
	void 진행자가_제목을_수정하면_반영되고_userModified가_true다() throws Exception {
		Long meetingId = createMeetingWithHost();

		mockMvc.perform(patch("/meetings/{meetingId}/title", meetingId)
						.header(HttpHeaders.AUTHORIZATION, bearer(HOST_ID))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title": "새 회의 제목"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.meetingId").value(meetingId))
				.andExpect(jsonPath("$.result.title").value("새 회의 제목"))
				.andExpect(jsonPath("$.result.userModified").value(true));
	}

	@Test
	void 종료된_회의도_제목을_수정할_수_있다() throws Exception {
		Meeting meeting = Meeting.of(1L, "회의");
		meeting.end();
		Long meetingId = meetingRepository.save(meeting).getId();
		meetingParticipantRepository.save(MeetingParticipant.of(meetingId, HOST_ID, ParticipantRole.HOST));

		mockMvc.perform(patch("/meetings/{meetingId}/title", meetingId)
						.header(HttpHeaders.AUTHORIZATION, bearer(HOST_ID))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title": "종료 후 수정한 제목"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.title").value("종료 후 수정한 제목"));
	}

	@Test
	void 진행자가_아니면_403과_도메인_에러코드를_반환한다() throws Exception {
		Long meetingId = createMeetingWithHost();

		mockMvc.perform(patch("/meetings/{meetingId}/title", meetingId)
						.header(HttpHeaders.AUTHORIZATION, bearer(HOST_ID + 1))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title": "새 회의 제목"}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("MEETING403_5"));
	}

	@Test
	void 존재하지_않는_회의면_404를_반환한다() throws Exception {
		mockMvc.perform(patch("/meetings/{meetingId}/title", 999_999L)
						.header(HttpHeaders.AUTHORIZATION, bearer(HOST_ID))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title": "새 회의 제목"}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("MEETING404_1"));
	}

	@Test
	void 제목이_공백이면_400을_반환한다() throws Exception {
		Long meetingId = createMeetingWithHost();

		mockMvc.perform(patch("/meetings/{meetingId}/title", meetingId)
						.header(HttpHeaders.AUTHORIZATION, bearer(HOST_ID))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title": "   "}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MEETING400_2"));
	}

	@Test
	void 토큰_없이_호출하면_401을_반환한다() throws Exception {
		Long meetingId = createMeetingWithHost();

		mockMvc.perform(patch("/meetings/{meetingId}/title", meetingId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title": "새 회의 제목"}
								"""))
				.andExpect(status().isUnauthorized());
	}

	private String bearer(Long userId) {
		return "Bearer " + jwtProvider.createAccessToken(userId);
	}
}
