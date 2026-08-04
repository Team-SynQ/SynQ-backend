package com.synq.backend.domain.reference.event;

/**
 * 링크 참고자료가 저장된 뒤 발행한다. 커밋 이후에 본문 추출과 인덱싱을 트리거한다.
 */
public record ReferenceLinkCreatedEvent(Long referenceId, Long projectId, String url) {
}
