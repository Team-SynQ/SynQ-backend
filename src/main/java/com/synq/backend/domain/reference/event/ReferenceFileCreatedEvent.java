package com.synq.backend.domain.reference.event;

/**
 * 파일 참고자료가 저장된 뒤 발행한다. 커밋 이후에 인덱싱을 트리거한다.
 *
 * @param extractedText 등록 요청에서 이미 뽑아둔 값. 리스너가 원본을 다시 파싱하지 않는다
 */
public record ReferenceFileCreatedEvent(Long referenceId, Long projectId, String extractedText) {
}
