package com.synq.backend.domain.project.service;

import com.synq.backend.domain.project.code.ProjectErrorCode;
import com.synq.backend.domain.project.dto.ProjectJoinRequestApproveResponse;
import com.synq.backend.domain.project.dto.ProjectJoinRequestListResponse;
import com.synq.backend.domain.project.dto.ProjectJoinRequestRejectResponse;
import com.synq.backend.domain.project.dto.ProjectJoinRequestResponse;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectJoinRequestStatus;
import com.synq.backend.domain.project.entity.ProjectJoinSettingSource;
import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.project.entity.ProjectMemberRole;
import com.synq.backend.domain.project.entity.ProjectParticipationRequest;
import com.synq.backend.domain.project.entity.ProjectParticipationRequestPerspective;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectParticipationRequestPerspectiveRepository;
import com.synq.backend.domain.project.repository.ProjectParticipationRequestRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.user.entity.Perspective;
import com.synq.backend.domain.user.entity.Role;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.global.apipayload.code.BaseCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class ProjectJoinRequestManageServiceTest extends PostgresTestContainer {

	@Autowired
	private ProjectService projectService;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;

	@Autowired
	private ProjectParticipationRequestRepository participationRequestRepository;

	@Autowired
	private ProjectParticipationRequestPerspectiveRepository perspectiveRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void OWNER는_PENDING_참여_요청만_이름과_요청_시각으로_조회한다() {
		User owner = saveUser("소유자", "manage-list-owner@synq.com");
		User firstUser = saveUser("첫 번째", "manage-list-first@synq.com");
		User secondUser = saveUser("두 번째", "manage-list-second@synq.com");
		User processedUser = saveUser("처리 완료", "manage-list-processed@synq.com");
		Project project = saveProjectWithOwner(owner);
		ProjectParticipationRequest first = savePending(project, firstUser);
		ProjectParticipationRequest second = savePending(project, secondUser);
		ProjectParticipationRequest processed = savePending(project, processedUser);
		processed.reject();

		ProjectJoinRequestListResponse response =
				projectService.findPendingJoinRequests(project.getId(), owner.getUserId());

		assertThat(response.pendingCount()).isEqualTo(2);
		assertThat(response.requests())
				.extracting(ProjectJoinRequestResponse::requestId)
				.containsExactly(first.getId(), second.getId());
		assertThat(response.requests())
				.extracting(ProjectJoinRequestResponse::name)
				.containsExactly("첫 번째", "두 번째");
		assertThat(response.requests())
				.allSatisfy(request -> assertThat(request.requestedAt()).isNotNull());
	}

	@Test
	void OWNER가_PENDING_요청을_승인하면_MEMBER를_생성하고_역할_관점_요청값은_보존한다() {
		User owner = saveUser("소유자", "manage-approve-owner@synq.com");
		User requester = saveUser("요청자", "manage-approve-requester@synq.com");
		Project project = saveProjectWithOwner(owner);
		ProjectParticipationRequest request = savePending(project, requester);
		perspectiveRepository.save(ProjectParticipationRequestPerspective.of(
				request.getId(), Perspective.TECH_RISK));

		ProjectJoinRequestApproveResponse response = projectService.approveJoinRequest(
				project.getId(), request.getId(), owner.getUserId());

		ProjectMember member = projectMemberRepository
				.findByProjectIdAndUserId(project.getId(), requester.getUserId())
				.orElseThrow();
		assertThat(request.getStatus()).isEqualTo(ProjectJoinRequestStatus.APPROVED);
		assertThat(member.getRole()).isEqualTo(ProjectMemberRole.MEMBER);
		assertThat(response.requestId()).isEqualTo(request.getId());
		assertThat(response.memberId()).isEqualTo(member.getId());
		assertThat(response.userId()).isEqualTo(requester.getUserId());
		assertThat(response.status()).isEqualTo("APPROVED");
		assertThat(response.joinedAt()).isEqualTo(member.getJoinedAt());
		assertThat(request.getRole()).isEqualTo(Role.DEV_TECH);
		assertThat(perspectiveRepository.findAllByJoinRequestIdOrderByIdAsc(request.getId()))
				.singleElement()
				.extracting(ProjectParticipationRequestPerspective::getPerspective)
				.isEqualTo(Perspective.TECH_RISK);
	}

	@Test
	void OWNER가_PENDING_요청을_거절하면_MEMBER를_생성하지_않는다() {
		User owner = saveUser("소유자", "manage-reject-owner@synq.com");
		User requester = saveUser("요청자", "manage-reject-requester@synq.com");
		Project project = saveProjectWithOwner(owner);
		ProjectParticipationRequest request = savePending(project, requester);

		ProjectJoinRequestRejectResponse response = projectService.rejectJoinRequest(
				project.getId(), request.getId(), owner.getUserId());

		assertThat(request.getStatus()).isEqualTo(ProjectJoinRequestStatus.REJECTED);
		assertThat(response.requestId()).isEqualTo(request.getId());
		assertThat(response.status()).isEqualTo("REJECTED");
		assertThat(projectMemberRepository.findByProjectIdAndUserId(project.getId(), requester.getUserId()))
				.isEmpty();
	}

	@Test
	void OWNER가_아니면_목록_조회와_승인_거절을_할_수_없다() {
		User owner = saveUser("소유자", "manage-forbidden-owner@synq.com");
		User member = saveUser("멤버", "manage-forbidden-member@synq.com");
		User requester = saveUser("요청자", "manage-forbidden-requester@synq.com");
		Project project = saveProjectWithOwner(owner);
		projectMemberRepository.save(ProjectMember.of(
				project.getId(), member.getUserId(), ProjectMemberRole.MEMBER));
		ProjectParticipationRequest request = savePending(project, requester);

		assertCode(ProjectErrorCode.NOT_PROJECT_OWNER,
				() -> projectService.findPendingJoinRequests(project.getId(), member.getUserId()));
		assertCode(ProjectErrorCode.NOT_PROJECT_OWNER,
				() -> projectService.approveJoinRequest(project.getId(), request.getId(), member.getUserId()));
		assertCode(ProjectErrorCode.NOT_PROJECT_OWNER,
				() -> projectService.rejectJoinRequest(project.getId(), request.getId(), member.getUserId()));
		assertThat(request.getStatus()).isEqualTo(ProjectJoinRequestStatus.PENDING);
	}

	@Test
	void 이미_처리된_요청은_다시_승인하거나_거절할_수_없다() {
		User owner = saveUser("소유자", "manage-processed-owner@synq.com");
		User approvedUser = saveUser("승인됨", "manage-processed-approved@synq.com");
		User rejectedUser = saveUser("거절됨", "manage-processed-rejected@synq.com");
		Project project = saveProjectWithOwner(owner);
		ProjectParticipationRequest approved = savePending(project, approvedUser);
		ProjectParticipationRequest rejected = savePending(project, rejectedUser);
		approved.approve();
		rejected.reject();

		assertCode(ProjectErrorCode.JOIN_REQUEST_ALREADY_PROCESSED,
				() -> projectService.approveJoinRequest(project.getId(), approved.getId(), owner.getUserId()));
		assertCode(ProjectErrorCode.JOIN_REQUEST_ALREADY_PROCESSED,
				() -> projectService.rejectJoinRequest(project.getId(), rejected.getId(), owner.getUserId()));
		assertThat(projectMemberRepository.findByProjectIdAndUserId(project.getId(), approvedUser.getUserId()))
				.isEmpty();
	}

	@Test
	void 존재하지_않거나_삭제된_프로젝트와_다른_프로젝트_요청은_404이다() {
		User owner = saveUser("소유자", "manage-not-found-owner@synq.com");
		User requester = saveUser("요청자", "manage-not-found-requester@synq.com");
		Project project = saveProjectWithOwner(owner);
		Project otherProject = saveProjectWithOwner(owner);
		Project deletedProject = saveProjectWithOwner(owner);
		ProjectParticipationRequest otherRequest = savePending(otherProject, requester);
		ProjectParticipationRequest deletedProjectRequest = savePending(deletedProject, requester);
		deletedProject.softDelete();

		assertCode(ProjectErrorCode.PROJECT_NOT_FOUND,
				() -> projectService.findPendingJoinRequests(Long.MAX_VALUE, owner.getUserId()));
		assertCode(ProjectErrorCode.PROJECT_NOT_FOUND,
				() -> projectService.rejectJoinRequest(
						deletedProject.getId(), deletedProjectRequest.getId(), owner.getUserId()));
		assertCode(ProjectErrorCode.JOIN_REQUEST_NOT_FOUND,
				() -> projectService.approveJoinRequest(project.getId(), Long.MAX_VALUE, owner.getUserId()));
		assertCode(ProjectErrorCode.JOIN_REQUEST_NOT_FOUND,
				() -> projectService.rejectJoinRequest(project.getId(), otherRequest.getId(), owner.getUserId()));
	}

	@Test
	void 프로젝트가_OWNER_포함_10명이면_승인할_수_없고_요청은_PENDING을_유지한다() {
		User owner = saveUser("소유자", "manage-limit-owner@synq.com");
		Project project = saveProjectWithOwner(owner);
		for (int index = 0; index < 9; index++) {
			User member = saveUser("멤버 " + index, "manage-limit-member-%d@synq.com".formatted(index));
			projectMemberRepository.save(ProjectMember.of(
					project.getId(), member.getUserId(), ProjectMemberRole.MEMBER));
		}
		User requester = saveUser("요청자", "manage-limit-requester@synq.com");
		ProjectParticipationRequest request = savePending(project, requester);

		assertCode(ProjectErrorCode.PROJECT_MEMBER_LIMIT_EXCEEDED,
				() -> projectService.approveJoinRequest(project.getId(), request.getId(), owner.getUserId()));

		assertThat(request.getStatus()).isEqualTo(ProjectJoinRequestStatus.PENDING);
		assertThat(projectMemberRepository.findByProjectIdAndUserId(project.getId(), requester.getUserId()))
				.isEmpty();
	}

	private void assertCode(BaseCode code, Runnable action) {
		assertThatThrownBy(action::run)
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(code));
	}

	private Project saveProjectWithOwner(User owner) {
		Project project = projectRepository.save(Project.of(owner.getUserId(), "SynQ", null));
		projectMemberRepository.save(ProjectMember.of(
				project.getId(), owner.getUserId(), ProjectMemberRole.OWNER));
		return project;
	}

	private ProjectParticipationRequest savePending(Project project, User requester) {
		return participationRequestRepository.save(ProjectParticipationRequest.pending(
				project.getId(),
				requester.getUserId(),
				ProjectJoinSettingSource.DEFAULT,
				Role.DEV_TECH,
				null
		));
	}

	private User saveUser(String name, String email) {
		return userRepository.save(User.ofLocal(name, email, "password-hash"));
	}
}
