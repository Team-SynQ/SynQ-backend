package com.synq.backend.domain.ai.rag.port;

import java.util.Optional;

/**
 * 구현(어댑터)은 reference 도메인에서 제공
 */
public interface ReferenceMaterialPort {

	Optional<String> findExtractedText(Long referenceMaterialId);

	/**
	 * 참고자료가 속한 프로젝트 ID. 검색 스코프이며 재인덱싱 시 필요하다.
	 * ai/rag 는 reference 테이블을 직접 조회하지 않으므로 이 Port 로 받는다.
	 */
	Optional<Long> findProjectId(Long referenceMaterialId);

	void markProcessing(Long referenceMaterialId);

	void markCompleted(Long referenceMaterialId);

	void markFailed(Long referenceMaterialId, String reason);
}
