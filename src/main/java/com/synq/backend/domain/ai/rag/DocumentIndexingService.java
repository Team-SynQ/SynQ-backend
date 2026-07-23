package com.synq.backend.domain.ai.rag;

import com.synq.backend.domain.ai.client.EmbeddingClient;
import com.synq.backend.domain.ai.rag.chunking.TextChunker;
import com.synq.backend.domain.ai.rag.entity.DocumentChunk;
import com.synq.backend.domain.ai.rag.port.ReferenceMaterialPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIndexingService implements DocumentIndexer {

	private final TextChunker chunker;
	private final EmbeddingClient embeddingClient;
	private final DocumentChunkWriter chunkWriter;
	private final ReferenceMaterialPort referenceMaterialPort;

	@Override
	@Async("indexingExecutor")
	public void indexAsync(Long referenceMaterialId, Long projectId, String extractedText) {
		try {
			index(referenceMaterialId, projectId, extractedText);
		} catch (RuntimeException e) {
			log.error("비동기 인덱싱 실패. referenceMaterialId={}", referenceMaterialId, e);
		}
	}

	/**
	 * 청킹 → 임베딩 → 저장. all-or-nothing 이다.
	 * 실패하면 청크를 하나도 남기지 않고 FAILED 로 표시한 뒤 예외를 다시 던진다
	 * 몇 번을 돌려도 결과가 같다(멱등).
	 */
	public void index(Long referenceMaterialId, Long projectId, String extractedText) {
		referenceMaterialPort.markProcessing(referenceMaterialId);
		try {
			List<String> contents = chunker.chunk(extractedText);
			if (contents.isEmpty()) {
				throw new IllegalArgumentException("청킹 결과가 비어 있습니다. 추출 텍스트를 확인하세요.");
			}

			// 임베딩이 끝난 뒤에야 청크를 만든다. 임베딩 없는 청크는 DB 에 존재한 적이 없다.
			List<float[]> embeddings = embeddingClient.embedDocuments(contents);
			if (embeddings.size() != contents.size()) {
				throw new IllegalStateException(
						"임베딩 개수(%d)가 청크 개수(%d)와 다릅니다."
								.formatted(embeddings.size(), contents.size()));
			}

			String model = embeddingClient.modelName();
			List<DocumentChunk> chunks = new ArrayList<>(contents.size());
			for (int i = 0; i < contents.size(); i++) {
				chunks.add(DocumentChunk.of(
						referenceMaterialId, projectId, i, contents.get(i), embeddings.get(i), model));
			}

			// 재처리일 수 있으므로 기존 청크를 지우고 교체한다.
			chunkWriter.replace(referenceMaterialId, chunks);

			referenceMaterialPort.markCompleted(referenceMaterialId);
			log.info("문서 인덱싱 완료. referenceMaterialId={}, chunks={}", referenceMaterialId, chunks.size());

		} catch (RuntimeException e) {
			log.error("문서 인덱싱 실패. referenceMaterialId={}", referenceMaterialId, e);
			// 정리와 상태 전이를 분리한다. 정리가 실패해도 markFailed 는 반드시 실행돼야
			// 문서가 PROCESSING 에 영원히 갇히지 않는다.
			cleanupQuietly(referenceMaterialId);
			referenceMaterialPort.markFailed(referenceMaterialId, e.getMessage());
			throw e;
		}
	}

	/** 실패 후 잔여 청크 정리. 이 정리가 실패해도 원래 인덱싱 예외를 가리지 않도록 삼킨다. */
	private void cleanupQuietly(Long referenceMaterialId) {
		try {
			chunkWriter.deleteAll(referenceMaterialId);
		} catch (RuntimeException cleanupError) {
			log.error("실패 후 청크 정리 실패. referenceMaterialId={}", referenceMaterialId, cleanupError);
		}
	}
}
