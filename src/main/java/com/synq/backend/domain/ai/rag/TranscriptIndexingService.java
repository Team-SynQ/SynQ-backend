package com.synq.backend.domain.ai.rag;

import com.synq.backend.domain.ai.client.EmbeddingClient;
import com.synq.backend.domain.ai.rag.chunking.TextChunker;
import com.synq.backend.domain.ai.rag.entity.MeetingTranscriptChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 회의 전사를 청킹·임베딩해 저장한다.
 *
 * <p>DocumentIndexingService 를 일반화하지 않고 따로 둔다. 저장 대상 테이블이 다르고,
 * 참고자료의 상태 전이(ReferenceMaterialPort)와 전사의 상태 전이가 서로 다른 수명주기라
 * 공통화하면 양쪽에 조건 분기가 생긴다. 구현체가 셋 이상이 되면 그때 추출한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranscriptIndexingService {

	private final TextChunker chunker;
	private final EmbeddingClient embeddingClient;
	private final TranscriptChunkWriter chunkWriter;
	private final TranscriptIndexStatusWriter statusWriter;

	@Async("indexingExecutor")
	public void indexAsync(Long meetingId, Long projectId, String transcriptText) {
		try {
			index(meetingId, projectId, transcriptText);
		} catch (RuntimeException e) {
			// 비동기 호출자에게 전달할 곳이 없다. 상태는 index() 안에서 이미 FAILED 로 기록됐다.
			log.error("비동기 전사 인덱싱 실패. meetingId={}", meetingId, e);
		}
	}

	/**
	 * 청킹 → 임베딩 → 저장. all-or-nothing 이다.
	 * 실패하면 청크를 하나도 남기지 않고 FAILED 로 표시한 뒤 예외를 다시 던진다.
	 *
	 * <p>회의 종료 후 전사 세그먼트는 수정할 수 없으므로(TranscriptSegmentService.update 가
	 * IN_PROGRESS 가 아니면 거부한다) 같은 회의를 몇 번 돌려도 결과가 같다.
	 */
	public void index(Long meetingId, Long projectId, String transcriptText) {
		statusWriter.markProcessing(meetingId, projectId);

		try {
			List<String> contents = chunker.chunk(transcriptText);
			if (contents.isEmpty()) {
				// 녹음하지 않은 회의는 정상 상황이다. 참고자료 흐름과 달리 예외로 다루지 않는다.
				statusWriter.markSkipped(meetingId);
				log.info("인덱싱할 전사가 없어 건너뜁니다. meetingId={}", meetingId);
				return;
			}

			// 임베딩이 끝난 뒤에야 청크를 만든다. 임베딩 없는 청크는 DB 에 존재한 적이 없다.
			List<float[]> embeddings = embeddingClient.embedDocuments(contents);
			if (embeddings.size() != contents.size()) {
				throw new IllegalStateException(
						"임베딩 개수(%d)가 청크 개수(%d)와 다릅니다."
								.formatted(embeddings.size(), contents.size()));
			}

			String model = embeddingClient.modelName();
			List<MeetingTranscriptChunk> chunks = new ArrayList<>(contents.size());
			for (int i = 0; i < contents.size(); i++) {
				chunks.add(MeetingTranscriptChunk.of(
						meetingId, projectId, i, contents.get(i), embeddings.get(i), model));
			}

			// 재인덱싱일 수 있으므로 기존 청크를 지우고 교체한다.
			chunkWriter.replace(meetingId, chunks);

			statusWriter.markCompleted(meetingId, chunks.size());
			log.info("전사 인덱싱 완료. meetingId={}, chunks={}", meetingId, chunks.size());

		} catch (RuntimeException e) {
			log.error("전사 인덱싱 실패. meetingId={}", meetingId, e);
			// 정리와 상태 전이를 분리한다. 정리가 실패해도 markFailed 는 반드시 실행돼야
			// 회의가 PROCESSING 에 영원히 갇히지 않는다.
			cleanupQuietly(meetingId);
			statusWriter.markFailed(meetingId, e.getMessage());
			throw e;
		}
	}

	/** 실패 후 잔여 청크 정리. 이 정리가 실패해도 원래 인덱싱 예외를 가리지 않도록 삼킨다. */
	private void cleanupQuietly(Long meetingId) {
		try {
			chunkWriter.deleteAll(meetingId);
		} catch (RuntimeException cleanupError) {
			log.error("실패 후 전사 청크 정리 실패. meetingId={}", meetingId, cleanupError);
		}
	}
}
