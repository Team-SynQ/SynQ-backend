package com.synq.backend.domain.ai.rag;

import com.synq.backend.domain.ai.rag.chunking.TextChunker;
import com.synq.backend.domain.ai.rag.entity.MeetingTranscriptChunk;
import com.synq.backend.domain.ai.rag.entity.MeetingTranscriptIndexStatus;
import com.synq.backend.domain.ai.rag.entity.TranscriptIndexStatus;
import com.synq.backend.domain.ai.rag.repository.MeetingTranscriptChunkRepository;
import com.synq.backend.domain.ai.rag.repository.MeetingTranscriptIndexStatusRepository;
import com.synq.backend.support.MeetingTranscriptTestFixture;
import com.synq.backend.support.PostgresTestContainer;
import com.synq.backend.global.apipayload.exception.GeneralException;
import com.synq.backend.support.StubEmbeddingClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TranscriptIndexingServiceTest extends PostgresTestContainer {

	@Autowired
	private MeetingTranscriptChunkRepository chunkRepository;

	@Autowired
	private MeetingTranscriptIndexStatusRepository statusRepository;

	// 직접 new 하면 @Transactional 프록시가 없어 파생 삭제 쿼리와 상태 커밋이 실패한다.
	// 반드시 빈으로 받는다.
	@Autowired
	private TranscriptChunkWriter chunkWriter;

	@Autowired
	private TranscriptIndexStatusWriter statusWriter;

	@Autowired
	private MeetingTranscriptTestFixture fixture;

	// updated_at 을 과거로 돌려야 stale 인계를 검증할 수 있다. 엔티티에는 setter 가 없다.
	@Autowired
	private JdbcTemplate jdbcTemplate;

	private StubEmbeddingClient embeddingClient;
	private TranscriptIndexingService service;
	private MeetingTranscriptTestFixture.Fixture meeting;

	@BeforeEach
	void setUp() {
		chunkRepository.deleteAll();
		statusRepository.deleteAll();
		meeting = fixture.create();
		embeddingClient = new StubEmbeddingClient();
		service = new TranscriptIndexingService(
				new TextChunker(800, 100), embeddingClient, chunkWriter, statusWriter);
	}

	// 이 클래스는 @Transactional 이 아니라 롤백되지 않는다. 남은 행은 컨테이너를 공유하는
	// 다음 테스트 클래스를 오염시킨다.
	@AfterEach
	void tearDown() {
		chunkRepository.deleteAll();
		statusRepository.deleteAll();
	}

	/** 800자를 넘겨 청크가 여러 개 나오도록 하는 텍스트. */
	private static String longTranscript() {
		return ("가".repeat(500) + "\n\n" + "나".repeat(500) + "\n\n" + "다".repeat(500));
	}

	private MeetingTranscriptIndexStatus statusOf(Long meetingId) {
		return statusRepository.findByMeetingId(meetingId).orElseThrow();
	}

	@Test
	void 청크를_저장하고_COMPLETED_로_전이한다() {
		service.index(meeting.meetingId(), meeting.projectId(), longTranscript());

		List<MeetingTranscriptChunk> chunks =
				chunkRepository.findByMeetingIdOrderByChunkIndexAsc(meeting.meetingId());

		assertThat(chunks).hasSizeGreaterThan(1);
		assertThat(chunks.get(0).getChunkIndex()).isZero();
		assertThat(chunks.get(1).getChunkIndex()).isEqualTo(1);
		assertThat(chunks.get(0).getEmbedding()).hasSize(768);
		assertThat(chunks.get(0).getEmbeddingModel()).isEqualTo("stub-embedding-model");
		assertThat(chunks).allSatisfy(chunk ->
				assertThat(chunk.getProjectId()).isEqualTo(meeting.projectId()));

		MeetingTranscriptIndexStatus status = statusOf(meeting.meetingId());
		assertThat(status.getStatus()).isEqualTo(TranscriptIndexStatus.COMPLETED);
		assertThat(status.getChunkCount()).isEqualTo(chunks.size());
	}

	@Test
	void 임베딩이_실패하면_청크를_남기지_않고_FAILED_로_전이한다() {
		embeddingClient.failNext();

		// 동기 호출자는 실패를 예외로 전달받는다. 그 와중에도 청크 정리와 FAILED 전이는 이뤄진다.
		assertThatThrownBy(() -> service.index(
				meeting.meetingId(), meeting.projectId(), longTranscript()))
				.isInstanceOf(RuntimeException.class);

		assertThat(chunkRepository.findByMeetingIdOrderByChunkIndexAsc(meeting.meetingId())).isEmpty();

		MeetingTranscriptIndexStatus status = statusOf(meeting.meetingId());
		assertThat(status.getStatus()).isEqualTo(TranscriptIndexStatus.FAILED);
		assertThat(status.getFailureReason()).isNotBlank();
		assertThat(status.getChunkCount()).isZero();
	}

	@Test
	void 전사가_비어_있으면_예외_없이_SKIPPED_다() {
		// 녹음하지 않은 회의는 정상 상황이다. 참고자료 흐름과 달리 예외를 던지지 않는다.
		service.index(meeting.meetingId(), meeting.projectId(), "   ");

		assertThat(chunkRepository.findByMeetingIdOrderByChunkIndexAsc(meeting.meetingId())).isEmpty();
		assertThat(statusOf(meeting.meetingId()).getStatus()).isEqualTo(TranscriptIndexStatus.SKIPPED);
	}

	@Test
	void 재인덱싱은_기존_청크를_지우고_다시_만든다() {
		service.index(meeting.meetingId(), meeting.projectId(), longTranscript());
		int firstCount = chunkRepository.findByMeetingIdOrderByChunkIndexAsc(meeting.meetingId()).size();

		service.index(meeting.meetingId(), meeting.projectId(), longTranscript());
		List<MeetingTranscriptChunk> chunks =
				chunkRepository.findByMeetingIdOrderByChunkIndexAsc(meeting.meetingId());

		// 멱등: 두 번 돌려도 청크 수가 같고 UNIQUE(meeting_id, chunk_index) 위반이 나지 않는다.
		assertThat(chunks).hasSize(firstCount);
		assertThat(statusOf(meeting.meetingId()).getStatus()).isEqualTo(TranscriptIndexStatus.COMPLETED);
	}

	@Test
	void 인덱싱이_진행_중이면_중복_실행을_거부한다() {
		// 회의 종료 이벤트 인덱싱이 도는 중에 수동 재인덱싱이 들어오는 상황이다.
		// 막지 않으면 두 파이프라인이 같은 meeting_id 의 청크를 동시에 replace 한다.
		statusWriter.markProcessing(meeting.meetingId(), meeting.projectId());

		assertThatThrownBy(() -> service.index(
				meeting.meetingId(), meeting.projectId(), longTranscript()))
				.isInstanceOf(GeneralException.class);

		// 진행 중이던 쪽의 상태를 건드리지 않는다.
		assertThat(statusOf(meeting.meetingId()).getStatus()).isEqualTo(TranscriptIndexStatus.PROCESSING);
	}

	@Test
	void 오래_PROCESSING_에_머문_회의는_인계받는다() {
		// 서버가 인덱싱 도중 죽으면 PROCESSING 행이 남는다. 인계하지 않으면 영영 복구할 수 없다.
		statusWriter.markProcessing(meeting.meetingId(), meeting.projectId());
		jdbcTemplate.update(
				"UPDATE meeting_transcript_index_status SET updated_at = now() - interval '1 hour' "
						+ "WHERE meeting_id = ?", meeting.meetingId());

		service.index(meeting.meetingId(), meeting.projectId(), longTranscript());

		assertThat(statusOf(meeting.meetingId()).getStatus()).isEqualTo(TranscriptIndexStatus.COMPLETED);
	}

	@Test
	void 실패_후_재인덱싱하면_실패_사유가_지워진다() {
		embeddingClient.failNext();
		assertThatThrownBy(() -> service.index(
				meeting.meetingId(), meeting.projectId(), longTranscript()))
				.isInstanceOf(RuntimeException.class);

		service.index(meeting.meetingId(), meeting.projectId(), longTranscript());

		MeetingTranscriptIndexStatus status = statusOf(meeting.meetingId());
		assertThat(status.getStatus()).isEqualTo(TranscriptIndexStatus.COMPLETED);
		assertThat(status.getFailureReason()).isNull();
	}
}
