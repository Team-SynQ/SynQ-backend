package com.synq.backend.domain.reference.service;

import com.synq.backend.domain.project.code.ProjectErrorCode;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.reference.dto.ReferenceListResponse;
import com.synq.backend.domain.reference.dto.ReferenceResponse;
import com.synq.backend.domain.reference.entity.ReferenceMaterial;
import com.synq.backend.domain.reference.repository.ReferenceMaterialRepository;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.global.apipayload.code.GeneralErrorCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

	private void validateUser(Long userId) {
		if (!userRepository.existsById(userId)) {
			throw new GeneralException(ProjectErrorCode.USER_NOT_FOUND);
		}
	}

	private Project findActiveProjectById(Long projectId) {
		return projectRepository.findById(projectId)
				.filter(project -> !project.isDeleted())
				.orElseThrow(() -> new GeneralException(ProjectErrorCode.PROJECT_NOT_FOUND));
	}
}
