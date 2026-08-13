package com.synq.backend.domain.project.service;

import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.entity.MeetingStatus;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.domain.project.code.ProjectErrorCode;
import com.synq.backend.domain.project.config.ProjectInvitationProperties;
import com.synq.backend.domain.project.dto.ProjectCreateRequest;
import com.synq.backend.domain.project.dto.ProjectCreateResponse;
import com.synq.backend.domain.project.dto.ProjectDetailResponse;
import com.synq.backend.domain.project.dto.ProjectInvitationInfoResponse;
import com.synq.backend.domain.project.dto.ProjectInvitationOwnerResponse;
import com.synq.backend.domain.project.dto.ProjectInvitationResponse;
import com.synq.backend.domain.project.dto.ProjectJoinRequestCreateRequest;
import com.synq.backend.domain.project.dto.ProjectJoinRequestCreateResponse;
import com.synq.backend.domain.project.dto.ProjectJoinRequestApproveResponse;
import com.synq.backend.domain.project.dto.ProjectJoinRequestListResponse;
import com.synq.backend.domain.project.dto.ProjectJoinRequestRejectResponse;
import com.synq.backend.domain.project.dto.ProjectJoinRequestResultResponse;
import com.synq.backend.domain.project.dto.ProjectJoinRequestResponse;
import com.synq.backend.domain.project.dto.ProjectJoinResponse;
import com.synq.backend.domain.project.dto.ProjectListResponse;
import com.synq.backend.domain.project.dto.ProjectMemberListResponse;
import com.synq.backend.domain.project.dto.ProjectMemberResponse;
import com.synq.backend.domain.project.dto.ProjectRolePerspectiveResponse;
import com.synq.backend.domain.project.dto.ProjectRolePerspectiveUpdateRequest;
import com.synq.backend.domain.project.dto.ProjectRolePerspectiveUpdateResponse;
import com.synq.backend.domain.project.dto.ProjectUpdateRequest;
import com.synq.backend.domain.project.dto.ProjectUpdateResponse;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.project.entity.ProjectMemberPerspective;
import com.synq.backend.domain.project.entity.ProjectMemberRole;
import com.synq.backend.domain.project.entity.ProjectJoinRequestStatus;
import com.synq.backend.domain.project.entity.ProjectJoinSettingSource;
import com.synq.backend.domain.project.entity.ProjectParticipationRequest;
import com.synq.backend.domain.project.entity.ProjectParticipationRequestPerspective;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectMemberPerspectiveRepository;
import com.synq.backend.domain.project.repository.ProjectParticipationRequestPerspectiveRepository;
import com.synq.backend.domain.project.repository.ProjectParticipationRequestRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.user.entity.Perspective;
import com.synq.backend.domain.user.entity.Role;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.dto.RoleProfileResponse;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.domain.user.service.RoleProfileService;
import com.synq.backend.domain.user.service.ProfileImageService;
import com.synq.backend.global.apipayload.code.GeneralErrorCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

	private static final long MAX_PROJECTS_PER_USER = 20;
	private static final long MAX_PROJECT_MEMBERS = 10;

	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final ProjectMemberPerspectiveRepository projectMemberPerspectiveRepository;
	private final ProjectParticipationRequestRepository participationRequestRepository;
	private final ProjectParticipationRequestPerspectiveRepository participationRequestPerspectiveRepository;
	private final MeetingRepository meetingRepository;
	private final UserRepository userRepository;
	private final RoleProfileService roleProfileService;
	private final ProfileImageService profileImageService;
	private final ProjectInvitationProperties projectInvitationProperties;

	@Transactional
	public ProjectCreateResponse create(Long userId, ProjectCreateRequest request) {
		validateUser(userId);
		validateUserProjectLimit(userId);

		Project project = projectRepository.save(Project.of(userId, request.title(), request.description()));
		ProjectMember owner = projectMemberRepository.save(
				ProjectMember.of(project.getId(), userId, ProjectMemberRole.OWNER));
		roleProfileService.findDefaultRoleProfile(userId)
				.ifPresent(profile -> saveProjectRolePerspective(
						owner,
						true,
						profile.role(),
						profile.detailRole(),
						profile.perspectives()
				));
		return ProjectCreateResponse.from(project);
	}

	@Transactional(readOnly = true)
	public ProjectDetailResponse findById(Long projectId, Long userId) {
		if (userId == null) {
			throw new GeneralException(GeneralErrorCode.UNAUTHORIZED);
		}
		validateUser(userId);

		Project project = findActiveProjectById(projectId);
		if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
			throw new GeneralException(ProjectErrorCode.NOT_PROJECT_MEMBER);
		}

		Meeting activeMeeting = meetingRepository
				.findByProjectIdInAndStatus(List.of(projectId), MeetingStatus.IN_PROGRESS)
				.stream()
				.max(Comparator.comparing(Meeting::getId))
				.orElse(null);
		// Project 엔티티 updatedAt 반환
		return ProjectDetailResponse.from(
				project,
				activeMeeting == null ? null : activeMeeting.getId(),
				activeMeeting == null ? null : activeMeeting.getStartedAt()
		);
	}

	@Transactional(readOnly = true)
	public ProjectMemberListResponse findMembers(Long projectId, Long userId) {
		if (userId == null) {
			throw new GeneralException(GeneralErrorCode.UNAUTHORIZED);
		}
		validateUser(userId);

		Project project = findActiveProjectById(projectId);
		if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
			throw new GeneralException(ProjectErrorCode.NOT_PROJECT_MEMBER);
		}

		List<ProjectMember> members =
				projectMemberRepository.findAllByProjectIdOrderByJoinedAtAsc(projectId);
		Map<Long, User> userById = userRepository.findAllById(
						members.stream().map(ProjectMember::getUserId).toList()
				).stream()
				.collect(java.util.stream.Collectors.toMap(User::getUserId, user -> user));
		List<ProjectMemberResponse> memberResponses = members.stream()
				.map(member -> ProjectMemberResponse.from(
						member,
						userById.get(member.getUserId()),
						userId
				))
				.toList();

		return ProjectMemberListResponse.from(
				project,
				Math.toIntExact(MAX_PROJECT_MEMBERS),
				memberResponses
		);
	}

	@Transactional(readOnly = true)
	public ProjectRolePerspectiveResponse findRolePerspective(Long projectId, Long userId) {
		if (userId == null) {
			throw new GeneralException(GeneralErrorCode.UNAUTHORIZED);
		}
		validateUser(userId);
		findActiveProjectById(projectId);

		ProjectMember member = findProjectMember(projectId, userId);
		if (member.isUseDefault() && member.getRoleCategory() == null) {
			return roleProfileService.findDefaultRoleProfile(userId)
					.map(profile -> ProjectRolePerspectiveResponse.from(
							true,
							profile.role(),
							profile.detailRole(),
							profile.perspectives()
					))
					.orElseGet(() -> ProjectRolePerspectiveResponse.from(
							true,
							null,
							null,
							List.of()
					));
		}

		return ProjectRolePerspectiveResponse.from(
				member.isUseDefault(),
				member.getRoleCategory(),
				member.getDetailRole(),
				findProjectMemberPerspectives(member.getId())
		);
	}

	@Transactional
	public ProjectRolePerspectiveUpdateResponse updateRolePerspective(
			Long projectId,
			Long userId,
			ProjectRolePerspectiveUpdateRequest request
	) {
		if (userId == null) {
			throw new GeneralException(GeneralErrorCode.UNAUTHORIZED);
		}
		validateUser(userId);
		findActiveProjectById(projectId);

		ProjectMember member = findProjectMember(projectId, userId);
		boolean useDefault = Boolean.TRUE.equals(request.useDefault());
		Role roleCategory;
		String detailRole;
		List<Perspective> perspectives;
		if (useDefault) {
			RoleProfileResponse defaultProfile = roleProfileService.findDefaultRoleProfile(userId)
					.orElse(null);
			roleCategory = defaultProfile == null ? null : defaultProfile.role();
			detailRole = defaultProfile == null ? null : defaultProfile.detailRole();
			perspectives = defaultProfile == null ? List.of() : defaultProfile.perspectives();
		} else {
			validateProjectRolePerspective(request);
			roleCategory = request.roleCategory();
			detailRole = request.detailRole();
			perspectives = request.perspectives() == null ? List.of() : request.perspectives();
		}

		projectMemberPerspectiveRepository.deleteAllByProjectMemberId(member.getId());
		projectMemberPerspectiveRepository.flush();
		saveProjectRolePerspective(member, useDefault, roleCategory, detailRole, perspectives);
		projectMemberRepository.flush();
		return ProjectRolePerspectiveUpdateResponse.from(member, perspectives);
	}

	@Transactional
	public ProjectUpdateResponse update(Long projectId, Long userId, ProjectUpdateRequest request) {
		if (userId == null) {
			throw new GeneralException(GeneralErrorCode.UNAUTHORIZED);
		}
		validateUser(userId);

		Project project = findActiveProjectById(projectId);
		if (!project.getOwnerId().equals(userId)) {
			throw new GeneralException(ProjectErrorCode.NOT_PROJECT_OWNER);
		}
		if (!request.isAnyFieldPresent()) {
			throw new GeneralException(GeneralErrorCode.BAD_REQUEST);
		}

		if (request.hasTitle()) {
			project.updateTitle(request.title());
		}
		if (request.hasDescription()) {
			project.updateDescription(request.description());
		}
		projectRepository.flush();
		return ProjectUpdateResponse.from(project);
	}

	@Transactional
	public void delete(Long projectId, Long userId) {
		if (userId == null) {
			throw new GeneralException(GeneralErrorCode.UNAUTHORIZED);
		}
		validateUser(userId);

		Project project = findActiveProjectById(projectId);
		if (!project.getOwnerId().equals(userId)) {
			throw new GeneralException(ProjectErrorCode.NOT_PROJECT_OWNER);
		}
		project.softDelete();
		projectRepository.flush();
	}

	@Transactional
	public void deleteMember(Long projectId, Long memberId, Long userId) {
		if (userId == null) {
			throw new GeneralException(GeneralErrorCode.UNAUTHORIZED);
		}
		validateUser(userId);

		Project project = findActiveProjectById(projectId);
		if (!project.getOwnerId().equals(userId)) {
			throw new GeneralException(ProjectErrorCode.NOT_PROJECT_OWNER);
		}

		ProjectMember member = projectMemberRepository.findByIdAndProjectId(memberId, projectId)
				.orElseThrow(() -> new GeneralException(ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND));
		if (member.getRole() == ProjectMemberRole.OWNER) {
			throw new GeneralException(ProjectErrorCode.CANNOT_DELETE_PROJECT_OWNER);
		}
		projectMemberRepository.delete(member);
	}

	@Transactional
	public void leave(Long projectId, Long userId) {
		if (userId == null) {
			throw new GeneralException(GeneralErrorCode.UNAUTHORIZED);
		}
		validateUser(userId);

		findActiveProjectById(projectId);
		ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
				.orElseThrow(() -> new GeneralException(ProjectErrorCode.NOT_PROJECT_MEMBER));
		if (member.getRole() == ProjectMemberRole.OWNER) {
			throw new GeneralException(ProjectErrorCode.PROJECT_OWNER_CANNOT_LEAVE);
		}
		projectMemberRepository.delete(member);
	}

	@Transactional(readOnly = true)
	public List<ProjectListResponse> findAll(Long userId) {
		if (userId == null) {
			throw new GeneralException(GeneralErrorCode.UNAUTHORIZED);
		}
		validateUser(userId);

		List<Long> projectIds = projectMemberRepository.findAllByUserId(userId).stream()
				.map(ProjectMember::getProjectId)
				.toList();
		if (projectIds.isEmpty()) {
			return List.of();
		}

		Map<Long, Meeting> recentMeetingByProjectId = new HashMap<>();
		meetingRepository.findRecentMeetingsByProjectIds(projectIds)
				.forEach(meeting -> recentMeetingByProjectId.merge(
						meeting.getProjectId(),
						meeting,
						(first, second) -> first.getId() > second.getId() ? first : second
				));
		Map<Long, Meeting> activeMeetingByProjectId = new HashMap<>();
		meetingRepository.findByProjectIdInAndStatus(projectIds, MeetingStatus.IN_PROGRESS)
				.forEach(meeting -> activeMeetingByProjectId.merge(
						meeting.getProjectId(),
						meeting,
						(first, second) -> first.getId() > second.getId() ? first : second
				));
		return projectRepository.findAllById(projectIds).stream()
				.map(project -> {
					Meeting recentMeeting = recentMeetingByProjectId.get(project.getId());
					Meeting activeMeeting = activeMeetingByProjectId.get(project.getId());
					LocalDateTime updatedAt = recentMeeting == null
							? project.getUpdatedAt()
							: latest(project.getUpdatedAt(), recentMeeting.getUpdatedAt());
					return ProjectListResponse.from(
							project,
							recentMeeting == null ? null : recentMeeting.getTitle(),
							activeMeeting == null ? null : activeMeeting.getId(),
							activeMeeting == null ? null : activeMeeting.getStartedAt(),
							updatedAt
					);
				})
				.sorted(Comparator.comparing(ProjectListResponse::updatedAt).reversed())
				.toList();
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
	public ProjectJoinRequestCreateResponse createJoinRequest(
			Long projectId,
			Long userId,
			ProjectJoinRequestCreateRequest request
	) {
		if (userId == null) {
			throw new GeneralException(GeneralErrorCode.UNAUTHORIZED);
		}
		validateUser(userId);

		Project project = findActiveProjectById(projectId);
		Project invitedProject = findProjectByInviteToken(request.inviteToken());
		if (!invitedProject.getId().equals(project.getId())) {
			throw new GeneralException(ProjectErrorCode.INVITATION_PROJECT_MISMATCH);
		}
		validateInvitationExpiration(invitedProject);

		if (projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
			throw new GeneralException(ProjectErrorCode.ALREADY_PROJECT_MEMBER);
		}
		if (participationRequestRepository.existsByProjectIdAndUserIdAndStatus(
				projectId,
				userId,
				ProjectJoinRequestStatus.PENDING
		)) {
			throw new GeneralException(ProjectErrorCode.JOIN_REQUEST_ALREADY_EXISTS);
		}
		if (projectMemberRepository.countByProjectId(projectId) >= MAX_PROJECT_MEMBERS) {
			throw new GeneralException(ProjectErrorCode.PROJECT_MEMBER_LIMIT_EXCEEDED);
		}
		validateJoinRequestRoleSetting(request);

		ProjectParticipationRequest participationRequest;
		try {
			participationRequest = participationRequestRepository.saveAndFlush(
					ProjectParticipationRequest.pending(
							projectId,
							userId,
							request.settingSource(),
							request.roleCategory(),
							request.detailRole()
					)
			);
		} catch (DataIntegrityViolationException exception) {
			throw new GeneralException(ProjectErrorCode.JOIN_REQUEST_ALREADY_EXISTS, exception);
		}

		participationRequestPerspectiveRepository.saveAll(
				request.perspectives().stream()
						.map(perspective -> ProjectParticipationRequestPerspective.of(
								participationRequest.getId(),
								perspective
						))
						.toList()
		);
		return ProjectJoinRequestCreateResponse.from(participationRequest);
	}

	@Transactional(readOnly = true)
	public ProjectJoinRequestListResponse findPendingJoinRequests(Long projectId, Long userId) {
		validateAuthenticatedUser(userId);
		validateUser(userId);

		Project project = findActiveProjectById(projectId);
		validateProjectOwner(project, userId);

		List<ProjectParticipationRequest> requests = participationRequestRepository
				.findAllByProjectIdAndStatusOrderByRequestedAtAscIdAsc(
						projectId,
						ProjectJoinRequestStatus.PENDING
				);
		Map<Long, User> userById = userRepository.findAllById(
					requests.stream()
							.map(ProjectParticipationRequest::getUserId)
							.distinct()
							.toList()
			).stream()
				.collect(Collectors.toMap(User::getUserId, user -> user));
		List<ProjectJoinRequestResponse> responses = requests.stream()
				.map(request -> ProjectJoinRequestResponse.from(
						request,
						findJoinRequestUser(userById, request.getUserId())
				))
				.toList();
		return ProjectJoinRequestListResponse.from(responses);
	}

	@Transactional(readOnly = true)
	public List<ProjectJoinRequestResultResponse> findMyJoinRequestResults(Long userId) {
		validateAuthenticatedUser(userId);
		validateUser(userId);

		List<ProjectParticipationRequest> requests = participationRequestRepository.findAllProcessedByUserId(
				userId,
				List.of(ProjectJoinRequestStatus.APPROVED, ProjectJoinRequestStatus.REJECTED)
		);
		Map<Long, Project> projectById = projectRepository.findAllById(
				requests.stream()
						.map(ProjectParticipationRequest::getProjectId)
						.distinct()
						.toList()
		).stream().collect(Collectors.toMap(Project::getId, project -> project));

		return requests.stream()
				.filter(request -> projectById.containsKey(request.getProjectId()))
				.map(request -> ProjectJoinRequestResultResponse.from(
						request,
						projectById.get(request.getProjectId())
				))
				.toList();
	}

	@Transactional
	public ProjectJoinRequestApproveResponse approveJoinRequest(
			Long projectId,
			Long requestId,
			Long userId
	) {
		validateAuthenticatedUser(userId);
		validateUser(userId);

		Project project = findActiveProjectByIdForUpdate(projectId);
		validateProjectOwner(project, userId);
		ProjectParticipationRequest request = findJoinRequest(projectId, requestId);
		validatePendingJoinRequest(request);

		if (projectMemberRepository.existsByProjectIdAndUserId(projectId, request.getUserId())) {
			throw new GeneralException(ProjectErrorCode.ALREADY_PROJECT_MEMBER);
		}
		validateUserProjectLimit(request.getUserId());
		if (projectMemberRepository.countByProjectId(projectId) >= MAX_PROJECT_MEMBERS) {
			throw new GeneralException(ProjectErrorCode.PROJECT_MEMBER_LIMIT_EXCEEDED);
		}

		request.approve();
		ProjectMember member = projectMemberRepository.save(
				ProjectMember.of(projectId, request.getUserId(), ProjectMemberRole.MEMBER));
		List<Perspective> perspectives = participationRequestPerspectiveRepository
				.findAllByJoinRequestIdOrderByIdAsc(request.getId()).stream()
				.map(ProjectParticipationRequestPerspective::getPerspective)
				.toList();
		saveProjectRolePerspective(
				member,
				request.getSettingSource() == ProjectJoinSettingSource.DEFAULT,
				request.getRole(),
				request.getDetailRole(),
				perspectives
		);
		return ProjectJoinRequestApproveResponse.from(request, member);
	}

	@Transactional
	public ProjectJoinRequestRejectResponse rejectJoinRequest(
			Long projectId,
			Long requestId,
			Long userId
	) {
		validateAuthenticatedUser(userId);
		validateUser(userId);

		Project project = findActiveProjectByIdForUpdate(projectId);
		validateProjectOwner(project, userId);
		ProjectParticipationRequest request = findJoinRequest(projectId, requestId);
		validatePendingJoinRequest(request);

		request.reject();
		return ProjectJoinRequestRejectResponse.from(request);
	}

	@Transactional
	public ProjectInvitationResponse createInvitation(Long projectId, Long userId) {
		if (userId == null) {
			throw new GeneralException(GeneralErrorCode.UNAUTHORIZED);
		}
		validateUser(userId);

		Project project = findActiveProjectById(projectId);
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
		ProjectMember ownerMember = projectMemberRepository
				.findByProjectIdAndRole(project.getId(), ProjectMemberRole.OWNER)
				.orElseThrow(() -> new GeneralException(ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND));
		User owner = userRepository.findById(ownerMember.getUserId())
				.orElseThrow(() -> new GeneralException(ProjectErrorCode.USER_NOT_FOUND));
		Role ownerRoleCategory = ownerMember.getRoleCategory();
		if (ownerMember.isUseDefault() && ownerRoleCategory == null) {
			ownerRoleCategory = roleProfileService.findDefaultRoleProfile(ownerMember.getUserId())
					.map(RoleProfileResponse::role)
					.orElse(null);
		}
		ProjectInvitationOwnerResponse ownerResponse = ProjectInvitationOwnerResponse.from(
				ownerMember,
				owner,
				profileImageService.toUrl(owner.getProfileImageKey()),
				ownerRoleCategory
		);

		return ProjectInvitationInfoResponse.from(
				project,
				currentMemberCount,
				Math.toIntExact(MAX_PROJECT_MEMBERS),
				alreadyJoined,
				ownerResponse
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

	private void validateAuthenticatedUser(Long userId) {
		if (userId == null) {
			throw new GeneralException(GeneralErrorCode.UNAUTHORIZED);
		}
	}

	private void validateProjectOwner(Project project, Long userId) {
		if (!project.getOwnerId().equals(userId)) {
			throw new GeneralException(ProjectErrorCode.NOT_PROJECT_OWNER);
		}
	}

	private ProjectParticipationRequest findJoinRequest(Long projectId, Long requestId) {
		return participationRequestRepository.findByIdAndProjectId(requestId, projectId)
				.orElseThrow(() -> new GeneralException(ProjectErrorCode.JOIN_REQUEST_NOT_FOUND));
	}

	private void validatePendingJoinRequest(ProjectParticipationRequest request) {
		if (request.getStatus() != ProjectJoinRequestStatus.PENDING) {
			throw new GeneralException(ProjectErrorCode.JOIN_REQUEST_ALREADY_PROCESSED);
		}
	}

	private User findJoinRequestUser(Map<Long, User> userById, Long userId) {
		User user = userById.get(userId);
		if (user == null) {
			throw new GeneralException(ProjectErrorCode.USER_NOT_FOUND);
		}
		return user;
	}

	private void validateUserProjectLimit(Long userId) {
		if (projectMemberRepository.countByUserId(userId) >= MAX_PROJECTS_PER_USER) {
			throw new GeneralException(ProjectErrorCode.USER_PROJECT_LIMIT_EXCEEDED);
		}
	}

	private Project findProjectByValidInviteToken(String inviteToken) {
		Project project = findProjectByInviteToken(inviteToken);
		validateInvitationExpiration(project);
		return project;
	}

	private Project findProjectByInviteToken(String inviteToken) {
		return projectRepository.findByInviteToken(inviteToken)
				.orElseThrow(() -> new GeneralException(ProjectErrorCode.INVITATION_NOT_FOUND));
	}

	private void validateInvitationExpiration(Project project) {
		if (project.getInviteTokenExpiresAt() == null
				|| !project.getInviteTokenExpiresAt().isAfter(LocalDateTime.now())) {
			throw new GeneralException(ProjectErrorCode.INVITATION_EXPIRED);
		}
	}

	private void validateJoinRequestRoleSetting(ProjectJoinRequestCreateRequest request) {
		if (request.settingSource() == null
				|| request.roleCategory() == null
				|| request.perspectives() == null
				|| request.perspectives().size() > 3
				|| new HashSet<>(request.perspectives()).size() != request.perspectives().size()
				|| request.perspectives().stream().anyMatch(Objects::isNull)
				|| (request.detailRole() != null && request.detailRole().length() > 30)
				|| (request.roleCategory() == Role.ETC && !StringUtils.hasText(request.detailRole()))) {
			throw new GeneralException(ProjectErrorCode.INVALID_JOIN_REQUEST_ROLE_SETTING);
		}
	}

	private void validateProjectRolePerspective(ProjectRolePerspectiveUpdateRequest request) {
		if (request.useDefault() == null
				|| request.roleCategory() == null
				|| (request.detailRole() != null && request.detailRole().length() > 30)
				|| (request.roleCategory() == Role.ETC && !StringUtils.hasText(request.detailRole()))
				|| (request.perspectives() != null && request.perspectives().size() > 3)
				|| (request.perspectives() != null
				&& new HashSet<>(request.perspectives()).size() != request.perspectives().size())
				|| (request.perspectives() != null && request.perspectives().stream().anyMatch(Objects::isNull))) {
			throw new GeneralException(ProjectErrorCode.INVALID_PROJECT_ROLE_PERSPECTIVE);
		}
	}

	private ProjectMember findProjectMember(Long projectId, Long userId) {
		return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
				.orElseThrow(() -> new GeneralException(ProjectErrorCode.NOT_PROJECT_MEMBER));
	}

	private List<Perspective> findProjectMemberPerspectives(Long projectMemberId) {
		return projectMemberPerspectiveRepository.findAllByProjectMemberIdOrderByIdAsc(projectMemberId)
				.stream()
				.map(ProjectMemberPerspective::getPerspective)
				.toList();
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

	private String generateInviteToken() {
		return UUID.randomUUID().toString();
	}

	private String buildInviteUrl(String inviteToken) {
		String frontendBaseUrl = projectInvitationProperties.frontendBaseUrl().replaceAll("/+$", "");
		return "%s/invite/%s".formatted(frontendBaseUrl, inviteToken);
	}

	private void saveProjectRolePerspective(
			ProjectMember member,
			boolean useDefault,
			Role roleCategory,
			String detailRole,
			List<Perspective> perspectives
	) {
		member.updateRolePerspective(useDefault, roleCategory, detailRole);
		projectMemberPerspectiveRepository.saveAll(
				perspectives.stream()
						.map(perspective -> ProjectMemberPerspective.of(member.getId(), perspective))
						.toList()
		);
	}

	private LocalDateTime latest(LocalDateTime first, LocalDateTime second) {
		return first.isAfter(second) ? first : second;
	}
}
