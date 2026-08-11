package com.synq.backend.domain.reference.service;

import com.synq.backend.domain.ai.rag.DocumentIndexer;
import com.synq.backend.domain.project.code.ProjectErrorCode;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.reference.code.ReferenceErrorCode;
import com.synq.backend.domain.reference.dto.ReferenceLinkCreateRequest;
import com.synq.backend.domain.reference.dto.ReferenceLinkCreateResponse;
import com.synq.backend.domain.reference.ReferenceLimits;
import com.synq.backend.domain.reference.dto.ReferenceFileCreateResponse;
import com.synq.backend.domain.reference.dto.ReferenceFileExtractionFailure;
import com.synq.backend.domain.reference.dto.ReferenceListResponse;
import com.synq.backend.domain.reference.dto.ReferenceNameUpdateRequest;
import com.synq.backend.domain.reference.dto.ReferenceNameUpdateResponse;
import com.synq.backend.domain.reference.dto.ReferenceResponse;
import com.synq.backend.domain.reference.entity.ReferenceMaterial;
import com.synq.backend.domain.reference.entity.ReferenceFileExtension;
import com.synq.backend.domain.reference.entity.ReferenceStatus;
import com.synq.backend.domain.reference.entity.ReferenceType;
import com.synq.backend.domain.reference.event.ReferenceFileDeletedEvent;
import com.synq.backend.domain.reference.event.ReferenceLinkCreatedEvent;
import com.synq.backend.domain.reference.file.ExtractedFile;
import com.synq.backend.domain.reference.file.FileExtractionFailureReason;
import com.synq.backend.domain.reference.file.FileTextExtractionException;
import com.synq.backend.domain.reference.file.FileTextExtractor;
import com.synq.backend.domain.reference.file.ReferenceFileExtractionException;
import com.synq.backend.domain.reference.link.LinkPreflightChecker;
import com.synq.backend.domain.reference.repository.ReferenceMaterialRepository;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReferenceService {

	private static final Logger log = LoggerFactory.getLogger(ReferenceService.class);
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
	private final ReferenceFileRegistrar referenceFileRegistrar;
	private final FileTextExtractor fileTextExtractor;

	// @Transactional 을 붙이지 않는다. 텍스트 추출이 상한 없는 CPU 작업이라
	// 트랜잭션 안에 두면 파싱 내내 DB 커넥션을 쥐게 된다. 저장은 ReferenceFileRegistrar 가 맡는다.
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

		// 멤버십을 추출보다 먼저 본다. 비멤버가 파싱을 유발할 수 없어야 한다.
		findActiveProjectById(projectId);
		if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
			throw new GeneralException(ProjectErrorCode.NOT_PROJECT_MEMBER);
		}

		List<ExtractedFile> extractedFiles = extractAll(validatedFiles);
		return referenceFileRegistrar.register(projectId, userId, uploader, extractedFiles);
	}

	/**
	 * 전부 추출한 뒤 실패가 하나라도 있으면 통째로 거절한다.
	 *
	 * <p>첫 실패에서 멈추지 않는 이유는, 5개를 올렸을 때 문제 파일을 한 번에 다 알려주기 위해서다.
	 * 나머지를 마저 파싱하는 비용을 치르지만 사용자가 왕복을 반복하지 않아도 된다.
	 */
	private List<ExtractedFile> extractAll(List<ValidatedFile> validatedFiles) {
		List<ExtractedFile> extracted = new ArrayList<>(validatedFiles.size());
		List<ReferenceFileExtractionFailure> failures = new ArrayList<>();
		for (ValidatedFile validatedFile : validatedFiles) {
			try (InputStream inputStream = validatedFile.file().getInputStream()) {
				String text = fileTextExtractor.extract(inputStream, validatedFile.name());
				extracted.add(new ExtractedFile(
						validatedFile.file(),
						validatedFile.name(),
						validatedFile.extension(),
						validatedFile.contentType(),
						text));
			} catch (FileTextExtractionException exception) {
				failures.add(new ReferenceFileExtractionFailure(
						validatedFile.name(), exception.getReason().name()));
			} catch (IOException exception) {
				log.warn("파일 스트림을 열지 못했습니다. fileName={}", validatedFile.name(), exception);
				failures.add(new ReferenceFileExtractionFailure(
						validatedFile.name(), FileExtractionFailureReason.CORRUPTED.name()));
			}
		}
		if (!failures.isEmpty()) {
			throw new ReferenceFileExtractionException(failures);
		}
		return extracted;
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
		if (reference.getType() == ReferenceType.FILE) {
			eventPublisher.publishEvent(new ReferenceFileDeletedEvent(
					referenceId, projectId, reference.getStorageKey()));
		}
	}

	@Transactional
	public ReferenceNameUpdateResponse updateName(
			Long projectId,
			Long referenceId,
			Long userId,
			ReferenceNameUpdateRequest request
	) {
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
			throw new GeneralException(ReferenceErrorCode.REFERENCE_UPDATE_FORBIDDEN);
		}

		String name = request.name();
		if (name == null || name.isBlank() || name.length() > 30) {
			throw new GeneralException(GeneralErrorCode.BAD_REQUEST);
		}
		reference.updateName(name);
		referenceMaterialRepository.flush();
		return ReferenceNameUpdateResponse.from(reference);
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
		if (referenceMaterialRepository.countByProjectId(projectId) >= ReferenceLimits.MAX_REFERENCES) {
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

		return ReferenceListResponse.from(ReferenceLimits.MAX_REFERENCES, responses);
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

	private record ValidatedFile(
			MultipartFile file,
			String name,
			ReferenceFileExtension extension,
			String contentType
	) {
	}
}
