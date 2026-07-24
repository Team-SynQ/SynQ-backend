package com.synq.backend.domain.project.service;

import com.synq.backend.domain.project.code.ProjectErrorCode;
import com.synq.backend.domain.project.config.ProjectInvitationProperties;
import com.synq.backend.domain.project.dto.ProjectCreateRequest;
import com.synq.backend.domain.project.dto.ProjectCreateResponse;
import com.synq.backend.domain.project.dto.ProjectInvitationInfoResponse;
import com.synq.backend.domain.project.dto.ProjectInvitationResponse;
import com.synq.backend.domain.project.dto.ProjectJoinResponse;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.project.entity.ProjectMemberRole;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.global.apipayload.code.GeneralErrorCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {

	private static final long MAX_PROJECTS_PER_USER = 20;
	private static final long MAX_PROJECT_MEMBERS = 10;

	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final UserRepository userRepository;
	private final ProjectInvitationProperties projectInvitationProperties;

	@Transactional
	public ProjectCreateResponse create(Long userId, ProjectCreateRequest request) {
		validateUser(userId);
		validateUserProjectLimit(userId);

		Project project = projectRepository.save(Project.of(userId, request.title(), request.description()));
		projectMemberRepository.save(ProjectMember.of(project.getId(), userId, ProjectMemberRole.OWNER));
		return ProjectCreateResponse.from(project);
	}

	@Transactional
	public ProjectJoinResponse join(Long userId, String inviteToken) {
		validateUser(userId);

		Project project = findProjectByValidInviteToken(inviteToken);

		return projectMemberRepository.findByProjectIdAndUserId(project.getId(), userId)
				.map(member -> ProjectJoinResponse.from(project, member, false))
				.orElseGet(() -> joinAsMember(project, userId));
	}

	@Transactional
	public ProjectInvitationResponse createInvitation(Long projectId, Long userId) {
		if (userId == null) {
			throw new GeneralException(GeneralErrorCode.UNAUTHORIZED);
		}
		validateUser(userId);

		Project project = projectRepository.findById(projectId)
				.orElseThrow(() -> new GeneralException(ProjectErrorCode.PROJECT_NOT_FOUND));
		if (!project.getOwnerId().equals(userId)) {
			throw new GeneralException(ProjectErrorCode.NOT_PROJECT_OWNER);
		}

		LocalDateTime now = LocalDateTime.now();
		if (project.getInviteToken() == null
				|| project.getInviteTokenExpiresAt() == null
				|| !project.getInviteTokenExpiresAt().isAfter(now)) {
			project.updateInvitation(
					generateInviteToken(),
					now.plusDays(projectInvitationProperties.expirationDays())
			);
		}

		return new ProjectInvitationResponse(
				buildInviteUrl(project.getInviteToken()),
				project.getInviteTokenExpiresAt()
		);
	}

	@Transactional(readOnly = true)
	public ProjectInvitationInfoResponse findInvitationInfo(String inviteToken, Long userId) {
		Project project = findProjectByValidInviteToken(inviteToken);
		int currentMemberCount = Math.toIntExact(projectMemberRepository.countByProjectId(project.getId()));
		boolean alreadyJoined = userId != null
				&& projectMemberRepository.findByProjectIdAndUserId(project.getId(), userId).isPresent();

		return ProjectInvitationInfoResponse.from(
				project,
				currentMemberCount,
				Math.toIntExact(MAX_PROJECT_MEMBERS),
				alreadyJoined
		);
	}

	private ProjectJoinResponse joinAsMember(Project project, Long userId) {
		validateUserProjectLimit(userId);
		if (projectMemberRepository.countByProjectId(project.getId()) >= MAX_PROJECT_MEMBERS) {
			throw new GeneralException(ProjectErrorCode.PROJECT_MEMBER_LIMIT_EXCEEDED);
		}

		ProjectMember member = projectMemberRepository.save(
				ProjectMember.of(project.getId(), userId, ProjectMemberRole.MEMBER));
		return ProjectJoinResponse.from(project, member, true);
	}

	private void validateUser(Long userId) {
		if (!userRepository.existsById(userId)) {
			throw new GeneralException(ProjectErrorCode.USER_NOT_FOUND);
		}
	}

	private void validateUserProjectLimit(Long userId) {
		if (projectMemberRepository.countByUserId(userId) >= MAX_PROJECTS_PER_USER) {
			throw new GeneralException(ProjectErrorCode.USER_PROJECT_LIMIT_EXCEEDED);
		}
	}

	private Project findProjectByValidInviteToken(String inviteToken) {
		Project project = projectRepository.findByInviteToken(inviteToken)
				.orElseThrow(() -> new GeneralException(ProjectErrorCode.INVITATION_NOT_FOUND));
		if (project.getInviteTokenExpiresAt() == null
				|| !project.getInviteTokenExpiresAt().isAfter(LocalDateTime.now())) {
			throw new GeneralException(ProjectErrorCode.INVITATION_EXPIRED);
		}
		return project;
	}

	private String generateInviteToken() {
		return UUID.randomUUID().toString();
	}

	private String buildInviteUrl(String inviteToken) {
		String frontendBaseUrl = projectInvitationProperties.frontendBaseUrl().replaceAll("/+$", "");
		return "%s/invite/%s".formatted(frontendBaseUrl, inviteToken);
	}
}
