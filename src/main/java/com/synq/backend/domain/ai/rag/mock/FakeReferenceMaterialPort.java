package com.synq.backend.domain.ai.rag.mock;

import com.synq.backend.domain.ai.rag.port.ReferenceMaterialPort;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * {@code ai.rag.reference-material.client=fake}일 때 사용하는 참고자료 포트 대역이다.
 *
 * <p>실제 참고자료 도메인 구현체가 준비되면 {@code client} 설정으로 교체한다.
 */
@Component
@ConditionalOnProperty(prefix = "ai.rag.reference-material", name = "client", havingValue = "fake")
public class FakeReferenceMaterialPort implements ReferenceMaterialPort {

	@Override
	public Optional<String> findExtractedText(Long referenceMaterialId) {
		return Optional.empty();
	}

	@Override
	public Optional<Long> findProjectId(Long referenceMaterialId) {
		return Optional.empty();
	}

	@Override
	public void markProcessing(Long referenceMaterialId) {
		// 실제 참고자료 상태 저장은 reference 도메인 구현체가 담당한다.
	}

	@Override
	public void markCompleted(Long referenceMaterialId) {
		// 실제 참고자료 상태 저장은 reference 도메인 구현체가 담당한다.
	}

	@Override
	public void markFailed(Long referenceMaterialId, String reason) {
		// 실제 참고자료 상태 저장은 reference 도메인 구현체가 담당한다.
	}
}
