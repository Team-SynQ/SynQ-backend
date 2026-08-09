package com.synq.backend.domain.meeting.controller;

import com.jayway.jsonpath.JsonPath;
import com.synq.backend.domain.auth.jwt.AccessTokenBlacklistService;
import com.synq.backend.domain.auth.jwt.JwtProvider;
import com.synq.backend.domain.meeting.entity.MeetingParticipant;
import com.synq.backend.domain.meeting.entity.ParticipantRole;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class MeetingControllerTest extends PostgresTestContainer {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MeetingParticipantRepository meetingParticipantRepository;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private AccessTokenBlacklistService accessTokenBlacklistService;

	@Test
	void 동의하면_미팅을_생성하고_생성자를_호스트로_등록한다() throws Exception {
		// project 테이블도 같은 시퀀스로 1부터 시작하는 id를 쓰므로, 다른 테스트가 만든 실제 프로젝트와
		// 겹치지 않도록(uq_meeting_project_active) 리터럴 대신 매번 새 값을 쓴다.
		long projectId = System.nanoTime();
		MvcResult result = mockMvc.perform(post("/projects/{projectId}/meetings", projectId)
						.header(HttpHeaders.AUTHORIZATION, bearer(10L))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"consentAgreed\": true}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.result.status").value("IN_PROGRESS"))
				.andExpect(jsonPath("$.result.startedAt").isNotEmpty())
				.andExpect(jsonPath("$.result.wsUrl").isNotEmpty())
				.andReturn();

		Long meetingId = ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.result.meetingId"))
				.longValue();

		List<MeetingParticipant> hosts = meetingParticipantRepository.findByMeetingIdAndRole(meetingId, ParticipantRole.HOST);
		assertThat(hosts).hasSize(1);
		assertThat(hosts.get(0).getUserId()).isEqualTo(10L);
	}

	@Test
	void 이미_진행_중인_회의가_있으면_409와_도메인_에러코드를_반환한다() throws Exception {
		long projectId = System.nanoTime();
		mockMvc.perform(post("/projects/{projectId}/meetings", projectId)
						.header(HttpHeaders.AUTHORIZATION, bearer(20L))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"consentAgreed\": true}"))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/projects/{projectId}/meetings", projectId)
						.header(HttpHeaders.AUTHORIZATION, bearer(20L))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"consentAgreed\": true}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("MEETING409_3"));
	}

	@Test
	void 동의하지_않으면_400과_도메인_에러코드를_반환한다() throws Exception {
		mockMvc.perform(post("/projects/{projectId}/meetings", 1L)
						.header(HttpHeaders.AUTHORIZATION, bearer(10L))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"consentAgreed\": false}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MEETING400_1"));
	}

	@Test
	void 토큰_없이_호출하면_401을_반환한다() throws Exception {
		mockMvc.perform(post("/projects/{projectId}/meetings", 1L)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"consentAgreed\": true}"))
				.andExpect(status().isUnauthorized());
	}

	private String bearer(Long userId) {
		return "Bearer " + jwtProvider.createAccessToken(userId);
	}
}
