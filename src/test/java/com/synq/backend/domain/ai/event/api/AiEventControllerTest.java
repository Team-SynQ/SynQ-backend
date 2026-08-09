package com.synq.backend.domain.ai.event.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.synq.backend.domain.auth.jwt.AccessTokenBlacklistService;
import com.synq.backend.domain.auth.jwt.JwtProvider;
import com.synq.backend.domain.ai.context.repository.LiveContextRepository;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.entity.MeetingParticipant;
import com.synq.backend.domain.meeting.entity.ParticipantRole;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class AiEventControllerTest extends PostgresTestContainer {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private MeetingRepository meetingRepository;

	@Autowired
	private MeetingParticipantRepository participantRepository;

	@Autowired
	private LiveContextRepository liveContextRepository;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private AccessTokenBlacklistService accessTokenBlacklistService;

	@BeforeEach
	void cleanUp() {
		liveContextRepository.deleteAll();
		participantRepository.deleteAll();
		meetingRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void 회의_참여자는_AI_결과_SSE를_구독할_수_있다() throws Exception {
		User user = userRepository.save(User.ofLocal("구독 사용자", "sse-user@synq.com", "encoded-password"));
		// project_id는 IN_PROGRESS 상태에서 유니크해야 하므로(uq_meeting_project_active), 매번 새 값을 쓴다.
		Meeting meeting = meetingRepository.save(Meeting.of(System.nanoTime(), "SSE 테스트 회의"));
		participantRepository.save(MeetingParticipant.of(meeting.getId(), user.getUserId(), ParticipantRole.HOST));

		MvcResult result = mockMvc.perform(get("/meetings/{meetingId}/ai-events", meeting.getId())
					.header(HttpHeaders.AUTHORIZATION, bearer(user.getUserId())))
				.andExpect(request().asyncStarted())
				.andExpect(header().string("X-Accel-Buffering", "no"))
				.andExpect(status().isOk())
				.andReturn();
		result.getRequest().getAsyncContext().complete();
	}

	@Test
	void 회의_비참여자는_AI_결과를_구독할_수_없다() throws Exception {
		User participant = userRepository.save(User.ofLocal("참여 사용자", "sse-member@synq.com", "encoded-password"));
		User outsider = userRepository.save(User.ofLocal("외부 사용자", "sse-outsider@synq.com", "encoded-password"));
		// project_id는 IN_PROGRESS 상태에서 유니크해야 하므로(uq_meeting_project_active), 매번 새 값을 쓴다.
		Meeting meeting = meetingRepository.save(Meeting.of(System.nanoTime(), "SSE 접근 제어 회의"));
		participantRepository.save(MeetingParticipant.of(meeting.getId(), participant.getUserId(), ParticipantRole.HOST));

		mockMvc.perform(get("/meetings/{meetingId}/ai-events", meeting.getId())
					.header(HttpHeaders.AUTHORIZATION, bearer(outsider.getUserId())))
				.andExpect(status().isForbidden());
	}

	@Test
	void 토큰이_없으면_SSE를_구독할_수_없다() throws Exception {
		mockMvc.perform(get("/meetings/{meetingId}/ai-events", 1L))
				.andExpect(status().isUnauthorized());
	}

	private String bearer(Long userId) {
		return "Bearer " + jwtProvider.createAccessToken(userId);
	}
}
