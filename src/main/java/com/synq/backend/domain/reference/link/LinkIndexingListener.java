package com.synq.backend.domain.reference.link;

import com.synq.backend.domain.ai.rag.DocumentIndexer;
import com.synq.backend.domain.ai.rag.port.ReferenceMaterialPort;
import com.synq.backend.domain.reference.event.ReferenceLinkCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 링크 등록 커밋 이후에 본문을 뽑아 인덱싱 파이프라인으로 넘긴다.
 *
 * <p>커밋 이후여야 하는 이유는, createLink 트랜잭션 안에서 비동기로 넘기면 커밋 전에 다른 스레드가
 * 시작되어 방금 저장한 행을 읽지 못하기 때문이다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LinkIndexingListener {

	private final LinkTextExtractor linkTextExtractor;
	private final DocumentIndexer documentIndexer;
	private final ReferenceMaterialPort referenceMaterialPort;

	@Async("linkFetchExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(ReferenceLinkCreatedEvent event) {
		try {
			linkTextExtractor.extract(event.url()).ifPresentOrElse(
					text -> documentIndexer.indexAsync(event.referenceId(), event.projectId(), text),
					() -> referenceMaterialPort.markFailed(
							event.referenceId(), "링크 본문을 추출하지 못했습니다."));
		} catch (RuntimeException e) {
			// 비동기 리스너의 예외는 밖으로 전파되지 않는다. 여기서 잡지 않으면
			// 참고자료가 UPLOADING 에 영원히 갇힌다.
			log.error("링크 인덱싱 트리거 실패. referenceId={}", event.referenceId(), e);
			referenceMaterialPort.markFailed(event.referenceId(), e.getMessage());
		}
	}
}
