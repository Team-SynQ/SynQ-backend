package com.synq.backend.support;

import com.synq.backend.domain.ai.rag.port.ReferenceMaterialPort;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** reference 도메인 구현 없이 파이프라인을 검증하기 위한 인메모리 대역. */
public class StubReferenceMaterialPort implements ReferenceMaterialPort {

	// @Async 인덱싱과 공유 빈으로 쓰일 때 동시 접근이 가능하므로 스레드 안전한 맵을 쓴다.
	private final Map<Long, String> texts = new ConcurrentHashMap<>();
	private final Map<Long, Long> projectIds = new ConcurrentHashMap<>();
	private final Map<Long, String> statuses = new ConcurrentHashMap<>();
	private final Map<Long, String> failureReasons = new ConcurrentHashMap<>();

	public void register(Long id, String extractedText) {
		register(id, 100L, extractedText);
	}

	public void register(Long id, Long projectId, String extractedText) {
		texts.put(id, extractedText);
		projectIds.put(id, projectId);
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
	public Optional<Long> findProjectId(Long referenceMaterialId) {
		return Optional.ofNullable(projectIds.get(referenceMaterialId));
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
