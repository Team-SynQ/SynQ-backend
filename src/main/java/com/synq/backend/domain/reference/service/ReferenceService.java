package com.synq.backend.domain.reference.service;

import com.synq.backend.domain.ai.rag.DocumentIndexer;
import com.synq.backend.domain.project.code.ProjectErrorCode;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.reference.code.ReferenceErrorCode;
import com.synq.backend.domain.reference.dto.ReferenceLinkCreateRequest;
import com.synq.backend.domain.reference.dto.ReferenceLinkCreateResponse;
import com.synq.backend.domain.reference.dto.ReferenceListResponse;
import com.synq.backend.domain.reference.dto.ReferenceResponse;
import com.synq.backend.domain.reference.entity.ReferenceMaterial;
import com.synq.backend.domain.reference.entity.ReferenceStatus;
import com.synq.backend.domain.reference.event.ReferenceLinkCreatedEvent;
import com.synq.backend.domain.reference.link.LinkPreflightChecker;
import com.synq.backend.domain.reference.repository.ReferenceMaterialRepository;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.global.apipayload.code.GeneralErrorCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReferenceService {

	private static final int MAX_REFERENCES = 10;

	private final ReferenceMaterialRepository referenceMaterialRepository;
	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final UserRepository userRepository;
	private final DocumentIndexer documentIndexer;
	private final LinkPreflightChecker linkPreflightChecker;
	private final ApplicationEventPublisher eventPublisher;

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
}
