package com.synq.backend.domain.reference.service;

import com.synq.backend.domain.project.code.ProjectErrorCode;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.reference.ReferenceLimits;
import com.synq.backend.domain.reference.code.ReferenceErrorCode;
import com.synq.backend.domain.reference.dto.ReferenceFileCreateResponse;
import com.synq.backend.domain.reference.dto.ReferenceFileResponse;
import com.synq.backend.domain.reference.entity.ReferenceFileExtension;
import com.synq.backend.domain.reference.entity.ReferenceMaterial;
import com.synq.backend.domain.reference.entity.ReferenceStatus;
import com.synq.backend.domain.reference.event.ReferenceFileCreatedEvent;
import com.synq.backend.domain.reference.file.ExtractedFile;
import com.synq.backend.domain.reference.repository.ReferenceMaterialRepository;
import com.synq.backend.domain.reference.storage.ReferenceStorage;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.global.apipayload.exception.GeneralException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 파일 참고자료의 락·제한 검사·S3 업로드·저장을 한 트랜잭션으로 묶는다.
 *
 * <p>ReferenceService 에서 분리한 이유는 텍스트 추출을 트랜잭션 밖으로 빼기 위해서다.
 * 추출은 상한 없는 CPU 작업이라 트랜잭션 안에 두면 파싱 내내 DB 커넥션을 쥐게 된다.
 */
@Component
@RequiredArgsConstructor
public class ReferenceFileRegistrar {

	private static final Logger log = LoggerFactory.getLogger(ReferenceFileRegistrar.class);

	private final ReferenceMaterialRepository referenceMaterialRepository;
	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final ReferenceStorage referenceStorage;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public ReferenceFileCreateResponse register(
			Long projectId,
			Long userId,
			User uploader,
			List<ExtractedFile> files
	) {
		findActiveProjectByIdForUpdate(projectId);
		// ReferenceService 가 추출 전에 이미 한 번 봤지만 여기서 또 본다. 그 검사는 비멤버가
		// 파싱을 유발하지 못하게 막는 것이고, 이 검사는 추출하는 수 초 사이에 멤버십이 해제된
		// 사용자가 등록을 마치는 것을 막는다. 목적이 달라 둘 중 하나로 대체되지 않는다.
		if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
			throw new GeneralException(ProjectErrorCode.NOT_PROJECT_MEMBER);
		}
		if (referenceMaterialRepository.countByProjectId(projectId) + files.size()
				> ReferenceLimits.MAX_REFERENCES) {
			throw new GeneralException(ReferenceErrorCode.REFERENCE_LIMIT_EXCEEDED);
		}

		List<String> uploadedStorageKeys = new ArrayList<>();
		AtomicBoolean compensated = new AtomicBoolean();
		boolean rollbackCompensationRegistered =
				registerRollbackCompensation(uploadedStorageKeys, compensated);
		try {
			List<ReferenceMaterial> references = new ArrayList<>();
			for (ExtractedFile extractedFile : files) {
				String storageKey = createStorageKey(projectId, extractedFile.extension());
				uploadedStorageKeys.add(storageKey);
				try (InputStream inputStream = extractedFile.file().getInputStream()) {
					referenceStorage.upload(
							storageKey,
							inputStream,
							extractedFile.file().getSize(),
							extractedFile.contentType()
					);
				}
				references.add(ReferenceMaterial.ofFile(
						projectId,
						userId,
						extractedFile.name(),
						extractedFile.file().getSize(),
						storageKey,
						extractedFile.extension(),
						ReferenceStatus.UPLOADING
				));
			}

			List<ReferenceMaterial> savedReferences =
					referenceMaterialRepository.saveAllAndFlush(references);
			// 파일마다 하나씩 발행한다. 리스트 하나로 묶으면 한 파일의 임베딩 실패가
			// 나머지 파일 처리에 얽힌다. 커밋 이후 FileIndexingListener 가 받는다.
			for (int index = 0; index < savedReferences.size(); index++) {
				eventPublisher.publishEvent(new ReferenceFileCreatedEvent(
						savedReferences.get(index).getId(), projectId, files.get(index).text()));
			}
			return new ReferenceFileCreateResponse(savedReferences.stream()
					.map(reference -> ReferenceFileResponse.from(reference, uploader))
					.toList());
		} catch (Exception exception) {
			if (!rollbackCompensationRegistered) {
				compensateOnce(uploadedStorageKeys, compensated);
			}
			throw new GeneralException(ReferenceErrorCode.REFERENCE_FILE_UPLOAD_FAILED, exception);
		}
	}

	private Project findActiveProjectByIdForUpdate(Long projectId) {
		return projectRepository.findByIdForUpdate(projectId)
				.filter(project -> !project.isDeleted())
				.orElseThrow(() -> new GeneralException(ProjectErrorCode.PROJECT_NOT_FOUND));
	}

	private String createStorageKey(Long projectId, ReferenceFileExtension extension) {
		return "references/" + projectId + "/" + UUID.randomUUID()
				+ "." + extension.name().toLowerCase(Locale.ROOT);
	}

	private boolean registerRollbackCompensation(List<String> storageKeys, AtomicBoolean compensated) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return false;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				if (status != TransactionSynchronization.STATUS_COMMITTED) {
					compensateOnce(storageKeys, compensated);
				}
			}
		});
		return true;
	}

	private void compensateOnce(List<String> storageKeys, AtomicBoolean compensated) {
		if (compensated.compareAndSet(false, true)) {
			compensateUploads(storageKeys);
		}
	}

	private void compensateUploads(List<String> storageKeys) {
		for (int index = storageKeys.size() - 1; index >= 0; index--) {
			String storageKey = storageKeys.get(index);
			try {
				referenceStorage.delete(storageKey);
			} catch (RuntimeException compensationFailure) {
				log.warn("참고자료 파일 보상 삭제 실패: storageKey={}", storageKey, compensationFailure);
			}
		}
	}
}
