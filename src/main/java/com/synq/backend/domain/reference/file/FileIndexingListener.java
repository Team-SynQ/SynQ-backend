package com.synq.backend.domain.reference.file;

import com.synq.backend.domain.ai.rag.DocumentIndexer;
import com.synq.backend.domain.ai.rag.port.ReferenceMaterialPort;
import com.synq.backend.domain.reference.event.ReferenceFileCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 파일 등록 커밋 이후에 인덱싱 파이프라인으로 넘긴다.
 *
 * <p>커밋 이후여야 하는 이유는, 등록 트랜잭션 안에서 비동기로 넘기면 커밋 전에 다른 스레드가
 * 시작되어 방금 저장한 행을 읽지 못하기 때문이다.
 *
 * <p>{@code @Async} 를 붙이지 않는다. indexAsync 가 이미 indexingExecutor 로 넘기므로
 * executor 를 두 번 갈아탈 이유가 없다. 링크 리스너는 자신이 네트워크 fetch 를 하느라
 * 전용 executor 가 필요했지만 여기서는 위임만 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileIndexingListener {

	private final DocumentIndexer documentIndexer;
	private final ReferenceMaterialPort referenceMaterialPort;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(ReferenceFileCreatedEvent event) {
		try {
			documentIndexer.indexAsync(event.referenceId(), event.projectId(), event.extractedText());
		} catch (RuntimeException e) {
			// 커밋은 이미 끝났다. 여기서 잡지 않으면 참고자료가 UPLOADING 에 영원히 갇힌다.
			log.error("파일 인덱싱 트리거 실패. referenceId={}", event.referenceId(), e);
			referenceMaterialPort.markFailed(event.referenceId(), e.getMessage());
		}
	}
}
