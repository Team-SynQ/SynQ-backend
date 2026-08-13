package com.synq.backend.domain.transcript.controller;

import com.synq.backend.domain.auth.jwt.AccessTokenBlacklistService;
import com.synq.backend.domain.auth.jwt.JwtProvider;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.entity.MeetingParticipant;
import com.synq.backend.domain.meeting.entity.ParticipantRole;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.domain.transcript.entity.MeetingRecordingSegment;
import com.synq.backend.domain.transcript.repository.MeetingRecordingSegmentRepository;
import com.synq.backend.domain.transcript.storage.RecordingStorage;
import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class MeetingRecordingControllerTest extends PostgresTestContainer {

	private static final long HOST_ID = 100L;
	private static final long MEMBER_ID = 200L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MeetingRepository meetingRepository;

	@Autowired
	private MeetingParticipantRepository meetingParticipantRepository;

	@Autowired
	private MeetingRecordingSegmentRepository segmentRepository;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private AccessTokenBlacklistService accessTokenBlacklistService;

	// 실제 S3 버킷 설정 없이도 presigned URL 발급 부분을 검증하기 위해 스토리지 자체를 목으로 대체한다.
	@MockitoBean
	private RecordingStorage recordingStorage;

	private Long createInProgressMeetingWithParticipants() {
		Long meetingId = meetingRepository.save(Meeting.of(System.nanoTime(), "회의")).getId();
		meetingParticipantRepository.save(MeetingParticipant.of(meetingId, HOST_ID, ParticipantRole.HOST));
		meetingParticipantRepository.save(MeetingParticipant.of(meetingId, MEMBER_ID, ParticipantRole.MEMBER));
		return meetingId;
	}

	@Test
	void 참가자는_세그먼트를_생성순서대로_presigned_URL과_함께_조회한다() throws Exception {
		Long meetingId = createInProgressMeetingWithParticipants();
		Long firstId = segmentRepository.save(MeetingRecordingSegment.of(meetingId, "recordings/1/a.webm")).getId();
		Long secondId = segmentRepository.save(MeetingRecordingSegment.of(meetingId, "recordings/1/b.webm")).getId();
		when(recordingStorage.presignedUrl(anyString(), any(Duration.class)))
				.thenReturn("https://example.com/presigned-url");

		mockMvc.perform(get("/meetings/{meetingId}/recordings", meetingId)
						.header(HttpHeaders.AUTHORIZATION, bearer(MEMBER_ID)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.length()").value(2))
				.andExpect(jsonPath("$.result[0].segmentId").value(firstId))
				.andExpect(jsonPath("$.result[0].url").value("https://example.com/presigned-url"))
				.andExpect(jsonPath("$.result[1].segmentId").value(secondId));
	}

	@Test
	void 세그먼트가_없으면_빈_배열을_반환한다() throws Exception {
		Long meetingId = createInProgressMeetingWithParticipants();

		mockMvc.perform(get("/meetings/{meetingId}/recordings", meetingId)
						.header(HttpHeaders.AUTHORIZATION, bearer(HOST_ID)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.length()").value(0));
	}

	@Test
	void 회의_참가자가_아니면_403과_도메인_에러코드를_반환한다() throws Exception {
		Long meetingId = createInProgressMeetingWithParticipants();

		mockMvc.perform(get("/meetings/{meetingId}/recordings", meetingId)
						.header(HttpHeaders.AUTHORIZATION, bearer(MEMBER_ID + 1)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("TRANSCRIPT403_3"));
	}

	@Test
	void 존재하지_않는_회의면_404를_반환한다() throws Exception {
		mockMvc.perform(get("/meetings/{meetingId}/recordings", 999_999L)
						.header(HttpHeaders.AUTHORIZATION, bearer(HOST_ID)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TRANSCRIPT404_1"));
	}

	@Test
	void 회의가_끝난_뒤에도_녹음_목록을_조회할_수_있다() throws Exception {
		Meeting meeting = Meeting.of(1L, "회의");
		meeting.end();
		Long meetingId = meetingRepository.save(meeting).getId();
		meetingParticipantRepository.save(MeetingParticipant.of(meetingId, HOST_ID, ParticipantRole.HOST));
		segmentRepository.save(MeetingRecordingSegment.of(meetingId, "recordings/1/a.webm"));
		when(recordingStorage.presignedUrl(anyString(), any(Duration.class)))
				.thenReturn("https://example.com/presigned-url");

		mockMvc.perform(get("/meetings/{meetingId}/recordings", meetingId)
						.header(HttpHeaders.AUTHORIZATION, bearer(HOST_ID)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.length()").value(1));
	}

	@Test
	void 토큰_없이_호출하면_401을_반환한다() throws Exception {
		Long meetingId = createInProgressMeetingWithParticipants();

		mockMvc.perform(get("/meetings/{meetingId}/recordings", meetingId))
				.andExpect(status().isUnauthorized());
	}

	private String bearer(Long userId) {
		return "Bearer " + jwtProvider.createAccessToken(userId);
	}
}
