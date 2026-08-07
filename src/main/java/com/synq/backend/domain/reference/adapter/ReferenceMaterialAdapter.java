package com.synq.backend.domain.reference.adapter;

import com.synq.backend.domain.ai.rag.port.ReferenceMaterialPort;
import com.synq.backend.domain.reference.entity.ReferenceMaterial;
import com.synq.backend.domain.reference.entity.ReferenceType;
import com.synq.backend.domain.reference.file.FileTextExtractor;
import com.synq.backend.domain.reference.link.LinkTextExtractor;
import com.synq.backend.domain.reference.repository.ReferenceMaterialRepository;
import com.synq.backend.domain.reference.storage.ReferenceStorage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * ai/rag 가 선언한 포트를 reference 도메인이 구현한다. 의존 방향은 reference → ai/rag 한쪽이다.
 *
 * <p>status 가 UPLOADING/AVAILABLE/READ_FAILED 3종뿐이라 포트가 가정한 4단계 수명주기를
 * 그대로 담지 못한다. 프론트에 이미 노출된 계약이라 enum 을 바꾸는 대신 여기서 매핑한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReferenceMaterialAdapter implements ReferenceMaterialPort {

	private final ReferenceMaterialRepository referenceMaterialRepository;
	private final LinkTextExtractor linkTextExtractor;
	private final ReferenceStorage referenceStorage;
	private final FileTextExtractor fileTextExtractor;

	@Override
	public Optional<String> findIndexableText(Long referenceMaterialId) {
		// @Transactional 을 붙이지 않는다. 붙이면 재fetch·재파싱 동안 DB 커넥션을 쥐고 있게 된다.
		return referenceMaterialRepository.findById(referenceMaterialId)
				.flatMap(reference -> reference.getType() == ReferenceType.LINK
						? linkTextExtractor.extract(reference.getUrl())
						: extractFromStorage(reference));
	}

	/**
	 * 파일은 S3 원본을 받아 다시 파싱한다. 재인덱싱은 임베딩 모델 교체나 장애 복구 때만 도는
	 * 개발자용 경로라, 상시 extracted_text 컬럼을 두는 대신 이 비용을 감수한다.
	 */
	private Optional<String> extractFromStorage(ReferenceMaterial reference) {
		try (InputStream inputStream = referenceStorage.download(reference.getStorageKey())) {
			return Optional.of(fileTextExtractor.extract(inputStream, reference.getName()));
		} catch (IOException | RuntimeException e) {
			log.warn("파일 참고자료 재파싱 실패. referenceMaterialId={}", reference.getId(), e);
			return Optional.empty();
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Long> findProjectId(Long referenceMaterialId) {
		return referenceMaterialRepository.findById(referenceMaterialId)
				.map(ReferenceMaterial::getProjectId);
	}

	@Override
	public void markProcessing(Long referenceMaterialId) {
		// UPLOADING 이 "등록됨" 과 "처리중" 을 겸하므로 전이할 상태가 없다.
	}

	@Override
	@Transactional
	public void markCompleted(Long referenceMaterialId) {
		// 인덱싱 도중 삭제되면 @SQLRestriction 때문에 조회되지 않는다.
		// 지운 자료의 상태를 되살리지 않는 것이 맞아 조용히 넘어간다.
		referenceMaterialRepository.findById(referenceMaterialId)
				.ifPresent(ReferenceMaterial::markAvailable);
	}

	@Override
	@Transactional
	public void markFailed(Long referenceMaterialId, String reason) {
		// 사유는 컬럼에 남기지 않는다. 화면에 노출할 계획이 없어 로그로 충분하다.
		log.warn("참고자료 인덱싱 실패. referenceMaterialId={}, reason={}", referenceMaterialId, reason);
		referenceMaterialRepository.findById(referenceMaterialId)
				.ifPresent(ReferenceMaterial::markReadFailed);
	}
}
