package com.synq.backend.domain.ai.rag.mock;

import com.synq.backend.domain.ai.rag.port.ReferenceMaterialPort;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 참고자료 도메인의 실제 구현 전까지, local 프로필에서 앱이 뜨도록 하는 임시 대역이다.
 * 실제 ReferenceMaterialPort 구현체(또는 PR #24 병합)가 들어오면 이 파일은 삭제한다.
 */
@Component
@Profile("local")
public class LocalReferenceMaterialPort implements ReferenceMaterialPort {

	@Override
	public Optional<String> findExtractedText(Long referenceMaterialId) {
		return Optional.empty();
	}

	@Override
	public void markProcessing(Long referenceMaterialId) {
	}

	@Override
	public void markCompleted(Long referenceMaterialId) {
	}

	@Override
	public void markFailed(Long referenceMaterialId, String reason) {
	}
}
