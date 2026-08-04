package com.synq.backend.domain.ai.rag.port;

import java.util.Optional;

/**
 * 구현(어댑터)은 reference 도메인에서 제공
 */
public interface ReferenceMaterialPort {

	/**
	 * 인덱싱할 텍스트를 조달한다.
	 *
	 * <p>추출 결과를 컬럼에 저장하지 않는다. 링크는 원본이 URL 이라 재fetch 로 재현되고,
	 * 청크 본문은 이미 document_chunk 에 있어 따로 두면 중복 저장이 된다.
	 */
	Optional<String> findIndexableText(Long referenceMaterialId);

	/**
	 * 참고자료가 속한 프로젝트 ID. 검색 스코프이며 재인덱싱 시 필요하다.
	 * ai/rag 는 reference 테이블을 직접 조회하지 않으므로 이 Port 로 받는다.
	 */
	Optional<Long> findProjectId(Long referenceMaterialId);

	void markProcessing(Long referenceMaterialId);

	void markCompleted(Long referenceMaterialId);

	void markFailed(Long referenceMaterialId, String reason);
}
