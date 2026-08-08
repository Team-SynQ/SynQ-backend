package com.synq.backend.domain.ai.rag.search;

import com.synq.backend.domain.ai.rag.entity.MeetingTranscriptChunk;
import com.synq.backend.domain.ai.rag.repository.MeetingTranscriptChunkRepository;
import com.synq.backend.support.MeetingTranscriptTestFixture;
import com.synq.backend.support.PostgresTestContainer;
import com.synq.backend.support.StubEmbeddingClient;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MeetingTranscriptChunkSearcherTest extends PostgresTestContainer {

	private static final String MODEL = "test-model";

	@Autowired
	private MeetingTranscriptChunkRepository repository;

	@Autowired
	private MeetingTranscriptTestFixture fixture;

	private MeetingTranscriptChunkSearcher searcher;
	private MeetingTranscriptTestFixture.Fixture meeting;

	/**
	 * 768차원 L2 정규화 벡터. first^2 + second^2 == 1 이어야 한다.
	 * StubEmbeddingClient 의 질의 벡터는 항상 (1, 0, 0, ...) 이다.
	 */
	private static float[] vector(float first, float second) {
		float[] v = new float[768];
		v[0] = first;
		v[1] = second;
		return v;
	}

	@BeforeEach
	void setUp() {
		repository.deleteAll();
		meeting = fixture.create();
		searcher = new MeetingTranscriptChunkSearcher(new StubEmbeddingClient(), repository);
	}

	@AfterEach
	void tearDown() {
		repository.deleteAll();
	}

	@Test
	void 질의와_가까운_청크를_회의_출처와_함께_반환한다() {
		repository.save(MeetingTranscriptChunk.of(
				meeting.meetingId(), meeting.projectId(), 2, "지난 회의 발화", vector(1.0f, 0.0f), MODEL));
		repository.flush();

		List<ChunkMatch> matches = searcher.search(
				new ChunkSearchQuery(meeting.projectId(), "인증 방식", 5, -1.0));

		assertThat(matches).hasSize(1);
		ChunkMatch match = matches.get(0);
		assertThat(match.content()).isEqualTo("지난 회의 발화");
		assertThat(match.source()).isEqualTo(ChunkSource.MEETING_TRANSCRIPT);
		assertThat(match.sourceId()).isEqualTo(meeting.meetingId());
		assertThat(match.chunkIndex()).isEqualTo(2);
		assertThat(match.similarity()).isCloseTo(1.0, Offset.offset(0.0001));
		assertThat(match.chunkId()).isNotNull();
	}

	@Test
	void 같은_프로젝트의_다른_회의도_검색된다() {
		// 예상 질문 추천이 "이 프로젝트의 이전 회의들" 을 검색하므로 회의 단위가 아니라 프로젝트 단위다.
		Long otherMeetingId = fixture.createMeeting(meeting);
		repository.save(MeetingTranscriptChunk.of(
				meeting.meetingId(), meeting.projectId(), 0, "이 회의", vector(1.0f, 0.0f), MODEL));
		repository.save(MeetingTranscriptChunk.of(
				otherMeetingId, meeting.projectId(), 0, "저 회의", vector(1.0f, 0.0f), MODEL));
		repository.flush();

		List<ChunkMatch> matches = searcher.search(
				new ChunkSearchQuery(meeting.projectId(), "질의", 5, -1.0));

		assertThat(matches).hasSize(2);
		assertThat(matches).extracting(ChunkMatch::sourceId)
				.containsExactlyInAnyOrder(meeting.meetingId(), otherMeetingId);
	}

	@Test
	void 제외한_회의의_전사_청크는_검색하지_않는다() {
		Long otherMeetingId = fixture.createMeeting(meeting);
		repository.save(MeetingTranscriptChunk.of(
				meeting.meetingId(), meeting.projectId(), 0, "현재 회의", vector(1.0f, 0.0f), MODEL));
		repository.save(MeetingTranscriptChunk.of(
				otherMeetingId, meeting.projectId(), 0, "이전 회의", vector(1.0f, 0.0f), MODEL));
		repository.flush();

		List<ChunkMatch> matches = searcher.search(
				new ChunkSearchQuery(meeting.projectId(), "질의", 5, -1.0, meeting.meetingId()));

		assertThat(matches).extracting(ChunkMatch::sourceId).containsExactly(otherMeetingId);
	}

	@Test
	void 임계값_미만은_제외한다() {
		// (0.6, 0.8) 과 질의 (1, 0) 의 코사인 유사도는 0.6 이다.
		repository.save(MeetingTranscriptChunk.of(
				meeting.meetingId(), meeting.projectId(), 0, "먼 청크", vector(0.6f, 0.8f), MODEL));
		repository.flush();

		List<ChunkMatch> matches = searcher.search(
				new ChunkSearchQuery(meeting.projectId(), "질의", 5, 0.7));

		assertThat(matches).isEmpty();
	}

	@Test
	void topK_만큼만_반환한다() {
		repository.save(MeetingTranscriptChunk.of(
				meeting.meetingId(), meeting.projectId(), 0, "A", vector(1.0f, 0.0f), MODEL));
		repository.save(MeetingTranscriptChunk.of(
				meeting.meetingId(), meeting.projectId(), 1, "B", vector(1.0f, 0.0f), MODEL));
		repository.save(MeetingTranscriptChunk.of(
				meeting.meetingId(), meeting.projectId(), 2, "C", vector(1.0f, 0.0f), MODEL));
		repository.flush();

		List<ChunkMatch> matches = searcher.search(
				new ChunkSearchQuery(meeting.projectId(), "질의", 2, -1.0));

		assertThat(matches).hasSize(2);
	}

	@Test
	void 결과가_없으면_빈_리스트다() {
		List<ChunkMatch> matches = searcher.search(
				new ChunkSearchQuery(meeting.projectId(), "질의", 5, -1.0));

		assertThat(matches).isEmpty();
	}
}
