package com.synq.backend.domain.reference.file;

import com.synq.backend.domain.reference.event.ReferenceFileDeletedEvent;
import com.synq.backend.domain.reference.storage.ReferenceStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * FILE 참고자료 삭제 커밋 이후의 S3 원본 정리.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileStorageCleanupListener {

	private final ReferenceStorage referenceStorage;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(ReferenceFileDeletedEvent event) {
		try {
			referenceStorage.delete(event.storageKey());
		} catch (RuntimeException exception) {
			// DB 삭제 성공 우선 정책. S3 정리 실패는 사용자 응답에 전파하지 않는다.
			log.warn(
					"참고자료 FILE S3 원본 삭제 실패. referenceId={}, projectId={}, storageKey={}, failureType={}",
					event.referenceId(),
					event.projectId(),
					event.storageKey(),
					exception.getClass().getSimpleName());
		}
	}
}
