package com.synq.backend.domain.reference.service;

import com.synq.backend.domain.ai.rag.DocumentIndexer;
import com.synq.backend.domain.project.code.ProjectErrorCode;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.reference.code.ReferenceErrorCode;
import com.synq.backend.domain.reference.dto.ReferenceLinkCreateRequest;
import com.synq.backend.domain.reference.dto.ReferenceLinkCreateResponse;
import com.synq.backend.domain.reference.dto.ReferenceFileCreateResponse;
import com.synq.backend.domain.reference.dto.ReferenceFileResponse;
import com.synq.backend.domain.reference.dto.ReferenceListResponse;
import com.synq.backend.domain.reference.dto.ReferenceResponse;
import com.synq.backend.domain.reference.entity.ReferenceMaterial;
import com.synq.backend.domain.reference.entity.ReferenceFileExtension;
import com.synq.backend.domain.reference.entity.ReferenceStatus;
import com.synq.backend.domain.reference.event.ReferenceLinkCreatedEvent;
import com.synq.backend.domain.reference.link.LinkPreflightChecker;
import com.synq.backend.domain.reference.repository.ReferenceMaterialRepository;
import com.synq.backend.domain.reference.storage.ReferenceStorage;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.global.apipayload.code.GeneralErrorCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReferenceService {

	private static final Logger log = LoggerFactory.getLogger(ReferenceService.class);
	private static final int MAX_REFERENCES = 10;
	private static final int MAX_FILES_PER_REQUEST = 5;
	private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
	private static final int MAX_FILE_NAME_LENGTH = 255;
	private static final Map<ReferenceFileExtension, Set<String>> CONTENT_TYPES = Map.of(
			ReferenceFileExtension.PDF, Set.of("application/pdf"),
			ReferenceFileExtension.DOCX, Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
			ReferenceFileExtension.PPTX, Set.of("application/vnd.openxmlformats-officedocument.presentationml.presentation"),
			ReferenceFileExtension.TXT, Set.of("text/plain")
	);

	private final ReferenceMaterialRepository referenceMaterialRepository;
	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final UserRepository userRepository;
	private final DocumentIndexer documentIndexer;
	private final LinkPreflightChecker linkPreflightChecker;
	private final ApplicationEventPublisher eventPublisher;
	private final ReferenceStorage referenceStorage;

	@Transactional
	public ReferenceFileCreateResponse createFiles(
			Long projectId,
			Long userId,
			List<MultipartFile> files
	) {
		if (userId == null) {
			throw new GeneralException(GeneralErrorCode.UNAUTHORIZED);
		}
		User uploader = validateUser(userId);
		List<ValidatedFile> validatedFiles = validateFiles(files);

		findActiveProjectByIdForUpdate(projectId);
		if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
			throw new GeneralException(ProjectErrorCode.NOT_PROJECT_MEMBER);
		}
		if (referenceMaterialRepository.countByProjectId(projectId) + validatedFiles.size() > MAX_REFERENCES) {
			throw new GeneralException(ReferenceErrorCode.REFERENCE_LIMIT_EXCEEDED);
		}

		List<String> uploadedStorageKeys = new ArrayList<>();
		AtomicBoolean compensated = new AtomicBoolean();
		boolean rollbackCompensationRegistered = registerRollbackCompensation(uploadedStorageKeys, compensated);
		try {
			List<ReferenceMaterial> references = new ArrayList<>();
			for (ValidatedFile validatedFile : validatedFiles) {
				String storageKey = createStorageKey(projectId, validatedFile.extension());
				uploadedStorageKeys.add(storageKey);
				try (InputStream inputStream = validatedFile.file().getInputStream()) {
					referenceStorage.upload(
							storageKey,
							inputStream,
							validatedFile.file().getSize(),
							validatedFile.contentType()
					);
				}
				references.add(ReferenceMaterial.ofFile(
						projectId,
						userId,
						validatedFile.name(),
						validatedFile.file().getSize(),
						storageKey,
						validatedFile.extension(),
						ReferenceStatus.UPLOADING
				));
			}

			List<ReferenceMaterial> savedReferences = referenceMaterialRepository.saveAllAndFlush(references);
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

	@Transactional
	public void delete(Long projectId, Long referenceId, Long userId) {
		if (userId == null) {
			throw new GeneralException(GeneralErrorCode.UNAUTHORIZED);
		}
		validateUser(userId);

		Project project = findActiveProjectById(projectId);
		if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
			throw new GeneralException(ProjectErrorCode.NOT_PROJECT_MEMBER);
		}

		ReferenceMaterial reference = referenceMaterialRepository
				.findByIdAndProjectId(referenceId, projectId)
				.orElseThrow(() -> new GeneralException(ReferenceErrorCode.REFERENCE_NOT_FOUND));
		if (!project.getOwnerId().equals(userId) && !reference.getUploaderId().equals(userId)) {
			throw new GeneralException(ReferenceErrorCode.REFERENCE_DELETE_FORBIDDEN);
		}

		reference.softDelete();
		documentIndexer.deleteIndex(referenceId);
	}

	@Transactional
	public ReferenceLinkCreateResponse createLink(
			Long projectId,
			Long userId,
			ReferenceLinkCreateRequest request
	) {
		if (userId == null) {
			throw new GeneralException(GeneralErrorCode.UNAUTHORIZED);
		}
		User uploader = validateUser(userId);

		// 락 없이 먼저 검증한다. 프리플라이트가 네트워크를 타므로 락을 쥔 채 기다리면
		// 같은 프로젝트의 동시 등록이 전부 그 뒤에 줄을 선다.
		// 멤버십을 먼저 보는 덕분에 비멤버가 URL 도달 여부를 알아내는 것도 막힌다.
		findActiveProjectById(projectId);
		if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
			throw new GeneralException(ProjectErrorCode.NOT_PROJECT_MEMBER);
		}
		linkPreflightChecker.check(request.url());

		findActiveProjectByIdForUpdate(projectId);
		if (referenceMaterialRepository.countByProjectId(projectId) >= MAX_REFERENCES) {
			throw new GeneralException(ReferenceErrorCode.REFERENCE_LIMIT_EXCEEDED);
		}

		ReferenceMaterial reference = referenceMaterialRepository.save(
				ReferenceMaterial.ofLink(
						projectId,
						userId,
						extractDomainName(request.url()),
						request.url(),
						ReferenceStatus.UPLOADING
				)
		);
		// 커밋 이후에 LinkIndexingListener 가 받는다.
		eventPublisher.publishEvent(new ReferenceLinkCreatedEvent(
				reference.getId(), projectId, reference.getUrl()));
		return ReferenceLinkCreateResponse.from(reference, uploader);
	}

	@Transactional(readOnly = true)
	public ReferenceListResponse findAll(Long projectId, Long userId) {
		if (userId == null) {
			throw new GeneralException(GeneralErrorCode.UNAUTHORIZED);
		}
		validateUser(userId);

		Project project = findActiveProjectById(projectId);
		if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
			throw new GeneralException(ProjectErrorCode.NOT_PROJECT_MEMBER);
		}

		List<ReferenceMaterial> references =
				referenceMaterialRepository.findAllByProjectIdOrderByCreatedAtDescIdDesc(projectId);
		Map<Long, User> uploaderById = userRepository.findAllById(
						references.stream()
								.map(ReferenceMaterial::getUploaderId)
								.distinct()
								.toList()
				).stream()
				.collect(Collectors.toMap(User::getUserId, user -> user));
		List<ReferenceResponse> responses = references.stream()
				.map(reference -> ReferenceResponse.from(
						reference,
						uploaderById.get(reference.getUploaderId()),
						project.getOwnerId().equals(userId)
								|| reference.getUploaderId().equals(userId)
				))
				.toList();

		return ReferenceListResponse.from(MAX_REFERENCES, responses);
	}

	private User validateUser(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new GeneralException(ProjectErrorCode.USER_NOT_FOUND));
	}

	private Project findActiveProjectById(Long projectId) {
		return projectRepository.findById(projectId)
				.filter(project -> !project.isDeleted())
				.orElseThrow(() -> new GeneralException(ProjectErrorCode.PROJECT_NOT_FOUND));
	}

	private Project findActiveProjectByIdForUpdate(Long projectId) {
		return projectRepository.findByIdForUpdate(projectId)
				.filter(project -> !project.isDeleted())
				.orElseThrow(() -> new GeneralException(ProjectErrorCode.PROJECT_NOT_FOUND));
	}

	private String extractDomainName(String url) {
		String host = URI.create(url).getHost().toLowerCase(Locale.ROOT);
		return host.startsWith("www.") ? host.substring(4) : host;
	}

	private List<ValidatedFile> validateFiles(List<MultipartFile> files) {
		if (files == null || files.isEmpty() || files.size() > MAX_FILES_PER_REQUEST) {
			throw new GeneralException(ReferenceErrorCode.INVALID_REFERENCE_FILE);
		}
		return files.stream()
				.map(this::validateFile)
				.toList();
	}

	private ValidatedFile validateFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new GeneralException(ReferenceErrorCode.INVALID_REFERENCE_FILE);
		}
		if (file.getSize() > MAX_FILE_SIZE) {
			throw new GeneralException(ReferenceErrorCode.REFERENCE_FILE_SIZE_EXCEEDED);
		}

		String name = sanitizeFileName(file.getOriginalFilename());
		ReferenceFileExtension extension = extractFileExtension(name);
		String contentType = resolveContentType(extension, file.getContentType());
		return new ValidatedFile(file, name, extension, contentType);
	}

	private String sanitizeFileName(String originalFilename) {
		if (originalFilename == null) {
			throw new GeneralException(ReferenceErrorCode.INVALID_REFERENCE_FILE);
		}
		String normalized = originalFilename.replace('\\', '/');
		String name = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
		if (name.isBlank() || name.length() > MAX_FILE_NAME_LENGTH) {
			throw new GeneralException(ReferenceErrorCode.INVALID_REFERENCE_FILE);
		}
		return name;
	}

	private ReferenceFileExtension extractFileExtension(String name) {
		int separator = name.lastIndexOf('.');
		if (separator <= 0 || separator == name.length() - 1) {
			throw new GeneralException(ReferenceErrorCode.UNSUPPORTED_REFERENCE_FILE);
		}
		try {
			return ReferenceFileExtension.valueOf(name.substring(separator + 1).toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new GeneralException(ReferenceErrorCode.UNSUPPORTED_REFERENCE_FILE, exception);
		}
	}

	private String resolveContentType(ReferenceFileExtension extension, String contentType) {
		String canonicalContentType = CONTENT_TYPES.get(extension).iterator().next();
		if (contentType == null || contentType.isBlank()) {
			return canonicalContentType;
		}
		int parameterSeparator = contentType.indexOf(';');
		String normalized = parameterSeparator >= 0
				? contentType.substring(0, parameterSeparator)
				: contentType;
		normalized = normalized.trim().toLowerCase(Locale.ROOT);
		if (normalized.equals("application/octet-stream")) {
			return canonicalContentType;
		}
		if (!CONTENT_TYPES.get(extension).contains(normalized)) {
			throw new GeneralException(ReferenceErrorCode.UNSUPPORTED_REFERENCE_FILE);
		}
		return normalized;
	}

	private String createStorageKey(Long projectId, ReferenceFileExtension extension) {
		return "references/" + projectId + "/" + UUID.randomUUID()
				+ "." + extension.name().toLowerCase(Locale.ROOT);
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

	private record ValidatedFile(
			MultipartFile file,
			String name,
			ReferenceFileExtension extension,
			String contentType
	) {
	}
}
