package com.synq.backend.domain.meeting.controller;

import com.jayway.jsonpath.JsonPath;
import com.synq.backend.domain.auth.jwt.AccessTokenBlacklistService;
import com.synq.backend.domain.auth.jwt.JwtProvider;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.entity.MeetingParticipant;
import com.synq.backend.domain.meeting.entity.ParticipantRole;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class MeetingParticipantControllerTest extends PostgresTestContainer {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MeetingRepository meetingRepository;

	@Autowired
	private MeetingParticipantRepository meetingParticipantRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private AccessTokenBlacklistService accessTokenBlacklistService;

	@Test
	void 활성_참여자만_역할과_함께_반환하고_나간_사람은_빠진다() throws Exception {
		User host = userRepository.save(User.ofLocal("호스트", "host-" + System.nanoTime() + "@synq.com", "password-hash"));
		User member = userRepository.save(User.ofLocal("멤버", "member-" + System.nanoTime() + "@synq.com", "password-hash"));
		User leftMember = userRepository.save(User.ofLocal("나간사람", "left-" + System.nanoTime() + "@synq.com", "password-hash"));

		Long meetingId = meetingRepository.save(Meeting.of(System.nanoTime(), "회의")).getId();
		meetingParticipantRepository.save(MeetingParticipant.of(meetingId, host.getUserId(), ParticipantRole.HOST));
		meetingParticipantRepository.save(MeetingParticipant.of(meetingId, member.getUserId(), ParticipantRole.MEMBER));
		MeetingParticipant left = meetingParticipantRepository.save(
				MeetingParticipant.of(meetingId, leftMember.getUserId(), ParticipantRole.MEMBER));
		left.leave();
		meetingParticipantRepository.save(left);

		MvcResult result = mockMvc.perform(get("/meetings/{meetingId}/participants", meetingId)
						.header(HttpHeaders.AUTHORIZATION, bearer(host.getUserId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.length()").value(2))
				.andReturn();

		List<Map<String, Object>> participants =
				JsonPath.read(result.getResponse().getContentAsString(), "$.result");
		assertThat(participants).extracting(p -> ((Number) p.get("userId")).longValue())
				.containsExactlyInAnyOrder(host.getUserId(), member.getUserId());

		Map<String, Object> hostResult = participants.stream()
				.filter(p -> ((Number) p.get("userId")).longValue() == host.getUserId())
				.findFirst().orElseThrow();
		assertThat(hostResult.get("role")).isEqualTo("HOST");
		assertThat(hostResult.get("name")).isEqualTo("호스트");

		Map<String, Object> memberResult = participants.stream()
				.filter(p -> ((Number) p.get("userId")).longValue() == member.getUserId())
				.findFirst().orElseThrow();
		assertThat(memberResult.get("role")).isEqualTo("MEMBER");
	}

	@Test
	void 현재_참여자가_아니면_403을_반환한다() throws Exception {
		Long meetingId = meetingRepository.save(Meeting.of(System.nanoTime(), "회의")).getId();
		meetingParticipantRepository.save(MeetingParticipant.of(meetingId, 100L, ParticipantRole.HOST));

		mockMvc.perform(get("/meetings/{meetingId}/participants", meetingId)
						.header(HttpHeaders.AUTHORIZATION, bearer(999L)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("MEETING403_4"));
	}

	@Test
	void 존재하지_않는_회의면_404를_반환한다() throws Exception {
		mockMvc.perform(get("/meetings/{meetingId}/participants", 999_999L)
						.header(HttpHeaders.AUTHORIZATION, bearer(100L)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("MEETING404_1"));
	}

	@Test
	void 토큰_없이_호출하면_401을_반환한다() throws Exception {
		Long meetingId = meetingRepository.save(Meeting.of(System.nanoTime(), "회의")).getId();
		meetingParticipantRepository.save(MeetingParticipant.of(meetingId, 100L, ParticipantRole.HOST));

		mockMvc.perform(get("/meetings/{meetingId}/participants", meetingId))
				.andExpect(status().isUnauthorized());
	}

	private String bearer(Long userId) {
		return "Bearer " + jwtProvider.createAccessToken(userId);
	}
}
