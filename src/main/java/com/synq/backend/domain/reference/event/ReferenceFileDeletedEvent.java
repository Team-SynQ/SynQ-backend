package com.synq.backend.domain.reference.event;

/**
 * FILE 참고자료의 Soft Delete와 인덱스 삭제가 커밋된 뒤 S3 원본 정리를 요청한다.
 */
public record ReferenceFileDeletedEvent(
		Long referenceId,
		Long projectId,
		String storageKey
) {
}
