package com.synq.backend.domain.transcript.controller;

import com.synq.backend.domain.auth.jwt.AccessTokenBlacklistService;
import com.synq.backend.domain.auth.jwt.JwtProvider;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.entity.MeetingParticipant;
import com.synq.backend.domain.meeting.entity.ParticipantRole;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.domain.transcript.entity.TranscriptSegment;
import com.synq.backend.domain.transcript.repository.TranscriptSegmentRepository;
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
class TranscriptSegmentControllerTest extends PostgresTestContainer {

	private static final long HOST_ID = 100L;
	private static final long MEMBER_ID = 200L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MeetingRepository meetingRepository;

	@Autowired
	private MeetingParticipantRepository meetingParticipantRepository;

	@Autowired
	private TranscriptSegmentRepository transcriptSegmentRepository;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private AccessTokenBlacklistService accessTokenBlacklistService;

	private Long createInProgressMeetingWithParticipants() {
		Long meetingId = meetingRepository.save(Meeting.of(1L, "회의")).getId();
		meetingParticipantRepository.save(MeetingParticipant.of(meetingId, HOST_ID, ParticipantRole.HOST));
		meetingParticipantRepository.save(MeetingParticipant.of(meetingId, MEMBER_ID, ParticipantRole.MEMBER));
		return meetingId;
	}

	private Long createSegment(Long meetingId) {
		return transcriptSegmentRepository.save(TranscriptSegment.of(meetingId, 0, 0, 1000, "오탈자")).getId();
	}

	@Test
	void 참가자는_호스트가_아니어도_전사를_수정할_수_있다() throws Exception {
		Long meetingId = createInProgressMeetingWithParticipants();
		Long segmentId = createSegment(meetingId);

		mockMvc.perform(patch("/meetings/{meetingId}/transcript-segments/{segmentId}", meetingId, segmentId)
						.header(HttpHeaders.AUTHORIZATION, bearer(MEMBER_ID))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content": "수정된 텍스트"}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.segmentId").value(segmentId))
				.andExpect(jsonPath("$.result.content").value("수정된 텍스트"))
				.andExpect(jsonPath("$.result.isModified").value(true));
	}

	@Test
	void 회의_참가자가_아니면_403과_도메인_에러코드를_반환한다() throws Exception {
		Long meetingId = createInProgressMeetingWithParticipants();
		Long segmentId = createSegment(meetingId);

		mockMvc.perform(patch("/meetings/{meetingId}/transcript-segments/{segmentId}", meetingId, segmentId)
						.header(HttpHeaders.AUTHORIZATION, bearer(MEMBER_ID + 1))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content": "수정된 텍스트"}"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("TRANSCRIPT403_2"));
	}

	@Test
	void 존재하지_않는_회의면_404를_반환한다() throws Exception {
		mockMvc.perform(patch("/meetings/{meetingId}/transcript-segments/{segmentId}", 999_999L, 1L)
						.header(HttpHeaders.AUTHORIZATION, bearer(HOST_ID))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content": "수정된 텍스트"}"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TRANSCRIPT404_1"));
	}

	@Test
	void 존재하지_않는_세그먼트면_404를_반환한다() throws Exception {
		Long meetingId = createInProgressMeetingWithParticipants();

		mockMvc.perform(patch("/meetings/{meetingId}/transcript-segments/{segmentId}", meetingId, 999_999L)
						.header(HttpHeaders.AUTHORIZATION, bearer(HOST_ID))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content": "수정된 텍스트"}"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TRANSCRIPT404_2"));
	}

	@Test
	void 진행_중이_아닌_회의면_409를_반환한다() throws Exception {
		Meeting meeting = Meeting.of(1L, "회의");
		meeting.end();
		Long meetingId = meetingRepository.save(meeting).getId();
		meetingParticipantRepository.save(MeetingParticipant.of(meetingId, HOST_ID, ParticipantRole.HOST));
		Long segmentId = createSegment(meetingId);

		mockMvc.perform(patch("/meetings/{meetingId}/transcript-segments/{segmentId}", meetingId, segmentId)
						.header(HttpHeaders.AUTHORIZATION, bearer(HOST_ID))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content": "수정된 텍스트"}"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("TRANSCRIPT409_2"));
	}

	@Test
	void 토큰_없이_호출하면_401을_반환한다() throws Exception {
		Long meetingId = createInProgressMeetingWithParticipants();
		Long segmentId = createSegment(meetingId);

		mockMvc.perform(patch("/meetings/{meetingId}/transcript-segments/{segmentId}", meetingId, segmentId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content": "수정된 텍스트"}"""))
				.andExpect(status().isUnauthorized());
	}

	private String bearer(Long userId) {
		return "Bearer " + jwtProvider.createAccessToken(userId);
	}
}
