package com.synq.backend.domain.ai.assistant.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.synq.backend.domain.ai.assistant.domain.SegmentHint;
import com.synq.backend.domain.ai.assistant.repository.SegmentHintRepository;
import com.synq.backend.domain.ai.context.repository.LiveContextRepository;
import com.synq.backend.domain.ai.rag.search.ChunkSearcher;
import com.synq.backend.domain.auth.jwt.AccessTokenBlacklistService;
import com.synq.backend.domain.auth.jwt.JwtProvider;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.entity.MeetingParticipant;
import com.synq.backend.domain.meeting.entity.ParticipantRole;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.domain.transcript.entity.TranscriptSegment;
import com.synq.backend.domain.transcript.repository.TranscriptSegmentRepository;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.support.PostgresTestContainer;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 3-hint 접근 제어 통합 테스트.
 *
 * <p>{@link HintControllerTest} 는 HintService 를 대역으로 둔 슬라이스라 권한 검증이 실제로 걸리는지,
 * 403 으로 매핑되는지 확인할 수 없다. 퇴장자 판정도 leftAt 컬럼이 있어야 성립하므로 DB 가 필요하다.
 */
@AutoConfigureMockMvc
class HintControllerIntegrationTest extends PostgresTestContainer {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private MeetingRepository meetingRepository;

	@Autowired
	private MeetingParticipantRepository meetingParticipantRepository;

	@Autowired
	private TranscriptSegmentRepository transcriptSegmentRepository;

	@Autowired
	private LiveContextRepository liveContextRepository;

	@Autowired
	private SegmentHintRepository segmentHintRepository;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private AccessTokenBlacklistService accessTokenBlacklistService;

	// 실제 검색은 쿼리 임베딩을 위해 외부 API 를 호출한다. 이 테스트의 관심사가 아니라 대역을 둔다.
	@MockitoBean
	private ChunkSearcher chunkSearcher;

	@BeforeEach
	void cleanUp() {
		given(chunkSearcher.search(any())).willReturn(List.of());
		// 세그먼트를 지우기 전에 힌트부터 지운다 (segment_id FK).
		segmentHintRepository.deleteAll();
		transcriptSegmentRepository.deleteAll();
		// 다른 테스트가 남긴 Live Context 가 meeting 삭제의 FK 제약에 걸린다.
		liveContextRepository.deleteAll();
		meetingParticipantRepository.deleteAll();
		meetingRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void 회의_참가자는_힌트를_생성한다() throws Exception {
		User user = saveUser("참가 사용자", "hint-participant@synq.com");
		Meeting meeting = saveMeetingWithParticipant(user.getUserId());
		Long segmentId = saveSegment(meeting.getId());

		mockMvc.perform(post("/meetings/{meetingId}/segments/{segmentId}/hints",
								meeting.getId(), segmentId)
						.header(HttpHeaders.AUTHORIZATION, bearer(user.getUserId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.meaning").isNotEmpty())
				.andExpect(jsonPath("$.result.myImpact").isNotEmpty())
				.andExpect(jsonPath("$.result.teamQuestion").isNotEmpty());
	}

	@Test
	void 회의_비참가자는_힌트를_생성할_수_없다() throws Exception {
		User participant = saveUser("참가 사용자", "hint-insider@synq.com");
		User outsider = saveUser("외부 사용자", "hint-outsider@synq.com");
		Meeting meeting = saveMeetingWithParticipant(participant.getUserId());
		Long segmentId = saveSegment(meeting.getId());

		mockMvc.perform(post("/meetings/{meetingId}/segments/{segmentId}/hints",
								meeting.getId(), segmentId)
						.header(HttpHeaders.AUTHORIZATION, bearer(outsider.getUserId())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("MEETING403_4"));
	}

	@Test
	void 회의에서_퇴장한_사용자는_힌트를_생성할_수_없다() throws Exception {
		User user = saveUser("퇴장 사용자", "hint-left@synq.com");
		Meeting meeting = saveMeetingWithParticipant(user.getUserId());
		Long segmentId = saveSegment(meeting.getId());

		MeetingParticipant participant = meetingParticipantRepository
				.findByMeetingIdAndUserId(meeting.getId(), user.getUserId())
				.get(0);
		participant.leave();
		meetingParticipantRepository.save(participant);

		mockMvc.perform(post("/meetings/{meetingId}/segments/{segmentId}/hints",
								meeting.getId(), segmentId)
						.header(HttpHeaders.AUTHORIZATION, bearer(user.getUserId())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("MEETING403_4"));
	}

	@Test
	void 비참가자에게는_세그먼트_존재_여부를_알려주지_않는다() throws Exception {
		User participant = saveUser("참가 사용자", "hint-probe-insider@synq.com");
		User outsider = saveUser("외부 사용자", "hint-probe-outsider@synq.com");
		Meeting meeting = saveMeetingWithParticipant(participant.getUserId());

		// 세그먼트가 없는데도 404 가 아니라 403 이어야 한다. 404/403 이 갈리면
		// 그 자체로 어떤 segmentId 가 실재하는지 훑어볼 수 있는 통로가 된다.
		mockMvc.perform(post("/meetings/{meetingId}/segments/{segmentId}/hints",
								meeting.getId(), 999_999L)
						.header(HttpHeaders.AUTHORIZATION, bearer(outsider.getUserId())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("MEETING403_4"));
	}

	@Test
	void 같은_세그먼트를_두_번_눌러도_힌트는_한_행만_남는다() throws Exception {
		User user = saveUser("반복 클릭 사용자", "hint-twice@synq.com");
		Meeting meeting = saveMeetingWithParticipant(user.getUserId());
		Long segmentId = saveSegment(meeting.getId());

		for (int i = 0; i < 2; i++) {
			mockMvc.perform(post("/meetings/{meetingId}/segments/{segmentId}/hints",
									meeting.getId(), segmentId)
							.header(HttpHeaders.AUTHORIZATION, bearer(user.getUserId())))
					.andExpect(status().isOk());
		}

		List<SegmentHint> hints = segmentHintRepository
				.findByMeetingIdAndUserIdOrderBySegmentIdAsc(meeting.getId(), user.getUserId());
		assertThat(hints).hasSize(1);
	}

	private User saveUser(String name, String email) {
		return userRepository.save(User.ofLocal(name, email, "encoded-password"));
	}

	private Meeting saveMeetingWithParticipant(Long userId) {
		Meeting meeting = meetingRepository.save(Meeting.of(1L, "3-hint 테스트 회의"));
		meetingParticipantRepository.save(
				MeetingParticipant.of(meeting.getId(), userId, ParticipantRole.HOST)
		);
		return meeting;
	}

	private Long saveSegment(Long meetingId) {
		return transcriptSegmentRepository.save(
				TranscriptSegment.of(meetingId, 0, 0, 900, "이번 스프린트 목표를 먼저 정합시다.")).getId();
	}

	private String bearer(Long userId) {
		return "Bearer " + jwtProvider.createAccessToken(userId);
	}
}
