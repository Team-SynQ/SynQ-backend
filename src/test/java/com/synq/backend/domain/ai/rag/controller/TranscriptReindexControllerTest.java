package com.synq.backend.domain.ai.rag.controller;

import com.synq.backend.domain.ai.rag.entity.TranscriptIndexStatus;
import com.synq.backend.domain.ai.rag.repository.MeetingTranscriptChunkRepository;
import com.synq.backend.domain.ai.rag.repository.MeetingTranscriptIndexStatusRepository;
import com.synq.backend.domain.auth.jwt.JwtProvider;
import com.synq.backend.support.MeetingTranscriptTestFixture;
import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 인가(프로젝트 멤버 검사)는 두지 않는다. 장애 복구용이라 회의가 속한 프로젝트의 멤버가 아닌
 * 개발자도 호출할 수 있어야 한다. 응답이 데이터를 반환하지 않아 정보 유출 경로도 아니다.
 * 다만 임베딩 비용을 유발하므로 인증은 요구한다.
 */
@AutoConfigureMockMvc
class TranscriptReindexControllerTest extends PostgresTestContainer {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtProvider jwtProvider;

	@Autowired
	private MeetingTranscriptChunkRepository chunkRepository;

	@Autowired
	private MeetingTranscriptIndexStatusRepository statusRepository;

	@Autowired
	private MeetingTranscriptTestFixture fixture;

	private MeetingTranscriptTestFixture.Fixture meeting;

	@BeforeEach
	void setUp() {
		chunkRepository.deleteAll();
		statusRepository.deleteAll();
		meeting = fixture.create();
	}

	@AfterEach
	void tearDown() {
		chunkRepository.deleteAll();
		statusRepository.deleteAll();
	}

	@Test
	void 재인덱싱하면_청크가_생기고_COMPLETED_가_된다() throws Exception {
		fixture.saveSegments(meeting.meetingId(), "가".repeat(500), "나".repeat(500));

		mockMvc.perform(post("/meetings/{meetingId}/transcript-reindex", meeting.meetingId())
						.header(HttpHeaders.AUTHORIZATION, bearer(meeting.hostId())))
				.andExpect(status().isOk());

		assertThat(chunkRepository.findByMeetingIdOrderByChunkIndexAsc(meeting.meetingId()))
				.isNotEmpty();
		assertThat(statusRepository.findByMeetingId(meeting.meetingId()).orElseThrow().getStatus())
				.isEqualTo(TranscriptIndexStatus.COMPLETED);
	}

	@Test
	void 녹음이_없는_회의는_SKIPPED_로_끝난다() {
		// 전사가 비어도 오류가 아니다. 호출자가 사람이므로 상태로 구분할 수 있어야 한다.
		org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
				mockMvc.perform(post("/meetings/{meetingId}/transcript-reindex", meeting.meetingId())
								.header(HttpHeaders.AUTHORIZATION, bearer(meeting.hostId())))
						.andExpect(status().isOk()));

		assertThat(statusRepository.findByMeetingId(meeting.meetingId()).orElseThrow().getStatus())
				.isEqualTo(TranscriptIndexStatus.SKIPPED);
	}

	@Test
	void 존재하지_않는_회의는_404_다() throws Exception {
		mockMvc.perform(post("/meetings/{meetingId}/transcript-reindex", -1L)
						.header(HttpHeaders.AUTHORIZATION, bearer(meeting.hostId())))
				.andExpect(status().isNotFound());
	}

	@Test
	void 인증_없이는_재인덱싱할_수_없다() throws Exception {
		// 임베딩 API 호출과 청크 재생성을 유발하므로 인증 없이 열어두면 비용 유발에 노출된다.
		mockMvc.perform(post("/meetings/{meetingId}/transcript-reindex", meeting.meetingId()))
				.andExpect(status().isUnauthorized());

		assertThat(chunkRepository.findByMeetingIdOrderByChunkIndexAsc(meeting.meetingId())).isEmpty();
	}

	private String bearer(Long userId) {
		return "Bearer " + jwtProvider.createAccessToken(userId);
	}
}
