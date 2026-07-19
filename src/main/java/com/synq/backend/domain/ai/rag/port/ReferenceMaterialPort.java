package com.synq.backend.domain.ai.rag.port;

import java.util.Optional;

/**
 * 구현(어댑터)은 reference 도메인에서 제공
 */
public interface ReferenceMaterialPort {

	Optional<String> findExtractedText(Long referenceMaterialId);

	void markProcessing(Long referenceMaterialId);

	void markCompleted(Long referenceMaterialId);

	void markFailed(Long referenceMaterialId, String reason);
}
