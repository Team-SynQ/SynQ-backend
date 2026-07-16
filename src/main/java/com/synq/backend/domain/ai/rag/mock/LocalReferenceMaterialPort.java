package com.synq.backend.domain.ai.rag.mock;

import com.synq.backend.domain.ai.rag.port.ReferenceMaterialPort;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 참고자료 도메인 구현 전까지 local 환경에서만 RAG 인덱싱 빈을 기동시키는 대역이다.
 * 실제 ReferenceMaterialPort 구현체가 준비되면 이 대역은 제거한다.
 */
@Component
@Profile("local")
public class LocalReferenceMaterialPort implements ReferenceMaterialPort {

	// 실제 reference 도메인이 준비되기 전까지 조회 요청은 빈 결과로 처리한다.
	private final Map<Long, String> extractedTexts = new ConcurrentHashMap<>();

	@Override
	public Optional<String> findExtractedText(Long referenceMaterialId) {
		return Optional.ofNullable(extractedTexts.get(referenceMaterialId));
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
