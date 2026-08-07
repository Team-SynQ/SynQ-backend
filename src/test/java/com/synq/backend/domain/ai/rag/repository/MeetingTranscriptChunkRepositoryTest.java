package com.synq.backend.domain.ai.rag.repository;

import com.synq.backend.domain.ai.rag.entity.MeetingTranscriptChunk;
import com.synq.backend.domain.ai.rag.search.ChunkSearchRow;
import com.synq.backend.support.MeetingTranscriptTestFixture;
import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MeetingTranscriptChunkRepositoryTest extends PostgresTestContainer {

	private static final String MODEL = "test-model";

	@Autowired
	private MeetingTranscriptChunkRepository repository;

	@Autowired
	private MeetingTranscriptTestFixture fixture;

	private MeetingTranscriptTestFixture.Fixture meeting;

	/** StubEmbeddingClient 의 질의 벡터는 항상 (1, 0, 0, ...) 이다. */
	private static float[] vector(float first, float second) {
		float[] v = new float[768];
		v[0] = first;
		v[1] = second;
		return v;
	}

	/** 위 vector(1, 0) 과 같은 방향의 질의 리터럴. */
	private static String queryVector() {
		return "[1.0" + ",0.0".repeat(767) + "]";
	}

	@BeforeEach
	void setUp() {
		repository.deleteAll();
		meeting = fixture.create();
	}

	// 이 클래스는 @Transactional 이 아니라 롤백되지 않는다. 남은 청크는 컨테이너를 공유하는
	// 다음 테스트 클래스의 UNIQUE(meeting_id, chunk_index) 를 깨뜨린다.
	@AfterEach
	void tearDown() {
		repository.deleteAll();
	}

	@Test
	void 청크를_순번_오름차순으로_조회한다() {
		repository.save(MeetingTranscriptChunk.of(
				meeting.meetingId(), meeting.projectId(), 1, "두 번째", vector(1.0f, 0.0f), MODEL));
		repository.save(MeetingTranscriptChunk.of(
				meeting.meetingId(), meeting.projectId(), 0, "첫 번째", vector(1.0f, 0.0f), MODEL));
		repository.flush();

		List<MeetingTranscriptChunk> chunks =
				repository.findByMeetingIdOrderByChunkIndexAsc(meeting.meetingId());

		assertThat(chunks).extracting(MeetingTranscriptChunk::getContent)
				.containsExactly("첫 번째", "두 번째");
		assertThat(chunks.get(0).getEmbedding()).hasSize(768);
		assertThat(chunks.get(0).getEmbeddingModel()).isEqualTo(MODEL);
	}

	// 파생 삭제 쿼리는 트랜잭션을 요구한다. 운영에서도 TranscriptChunkWriter 가 @Transactional 로
	// 감싸 호출하므로, 이 메서드에만 붙이는 것이 실제 사용 방식과 같다.
	@Test
	@Transactional
	void 회의_단위로_청크를_지운다() {
		repository.save(MeetingTranscriptChunk.of(
				meeting.meetingId(), meeting.projectId(), 0, "지울 청크", vector(1.0f, 0.0f), MODEL));
		repository.flush();

		repository.deleteByMeetingId(meeting.meetingId());

		assertThat(repository.findByMeetingIdOrderByChunkIndexAsc(meeting.meetingId())).isEmpty();
	}

	@Test
	void 프로젝트_스코프_밖의_청크는_검색되지_않는다() {
		MeetingTranscriptTestFixture.Fixture other = fixture.create();
		repository.save(MeetingTranscriptChunk.of(
				meeting.meetingId(), meeting.projectId(), 0, "내 프로젝트", vector(1.0f, 0.0f), MODEL));
		repository.save(MeetingTranscriptChunk.of(
				other.meetingId(), other.projectId(), 0, "남의 프로젝트", vector(1.0f, 0.0f), MODEL));
		repository.flush();

		List<ChunkSearchRow> rows = repository.searchByProject(
				meeting.projectId(), queryVector(), -1.0, 10);

		assertThat(rows).hasSize(1);
		assertThat(rows.get(0).getContent()).isEqualTo("내 프로젝트");
		// 전사 청크의 출처는 회의다. 참고자료 검색과 같은 projection 을 쓰되 의미가 다르다.
		assertThat(rows.get(0).getSourceId()).isEqualTo(meeting.meetingId());
	}
}
