package com.synq.backend.support;

import com.synq.backend.domain.ai.rag.port.ReferenceMaterialPort;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** reference 도메인 구현 없이 파이프라인을 검증하기 위한 인메모리 대역. */
public class StubReferenceMaterialPort implements ReferenceMaterialPort {

	private final Map<Long, String> texts = new HashMap<>();
	private final Map<Long, String> statuses = new HashMap<>();
	private final Map<Long, String> failureReasons = new HashMap<>();

	public void register(Long id, String extractedText) {
		texts.put(id, extractedText);
		statuses.put(id, "PENDING");
	}

	public String statusOf(Long id) {
		return statuses.get(id);
	}

	public String failureReasonOf(Long id) {
		return failureReasons.get(id);
	}

	@Override
	public Optional<String> findExtractedText(Long referenceMaterialId) {
		return Optional.ofNullable(texts.get(referenceMaterialId));
	}

	@Override
	public void markProcessing(Long referenceMaterialId) {
		statuses.put(referenceMaterialId, "PROCESSING");
	}

	@Override
	public void markCompleted(Long referenceMaterialId) {
		statuses.put(referenceMaterialId, "COMPLETED");
	}

	@Override
	public void markFailed(Long referenceMaterialId, String reason) {
		statuses.put(referenceMaterialId, "FAILED");
		failureReasons.put(referenceMaterialId, reason);
	}
}
