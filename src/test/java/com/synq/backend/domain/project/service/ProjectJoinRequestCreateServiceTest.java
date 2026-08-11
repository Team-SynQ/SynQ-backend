package com.synq.backend.domain.project.service;

import com.synq.backend.domain.project.code.ProjectErrorCode;
import com.synq.backend.domain.project.dto.ProjectJoinRequestCreateRequest;
import com.synq.backend.domain.project.dto.ProjectJoinRequestCreateResponse;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectJoinRequestStatus;
import com.synq.backend.domain.project.entity.ProjectJoinSettingSource;
import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.project.entity.ProjectMemberRole;
import com.synq.backend.domain.project.entity.ProjectParticipationRequest;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectParticipationRequestPerspectiveRepository;
import com.synq.backend.domain.project.repository.ProjectParticipationRequestRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.user.entity.Perspective;
import com.synq.backend.domain.user.entity.Role;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.global.apipayload.exception.GeneralException;
import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class ProjectJoinRequestCreateServiceTest extends PostgresTestContainer {

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

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void 참여_요청을_PENDING으로_저장하고_멤버는_생성하지_않는다() {
		User owner = saveUser("request-owner@synq.com");
		User requester = saveUser("request-user@synq.com");
		String token = UUID.randomUUID().toString();
		Project project = saveProjectWithOwner(owner, token, LocalDateTime.now().plusDays(7));
		ProjectJoinRequestCreateRequest request = request(
				token,
				ProjectJoinSettingSource.PROJECT_CUSTOM,
				Role.ETC,
				"백엔드 개발",
				List.of(Perspective.TECH_RISK, Perspective.ACTION_ITEM)
		);

		ProjectJoinRequestCreateResponse response =
				projectService.createJoinRequest(project.getId(), requester.getUserId(), request);

		ProjectParticipationRequest saved = participationRequestRepository.findById(response.requestId()).orElseThrow();
		assertThat(response.projectId()).isEqualTo(project.getId());
		assertThat(response.status()).isEqualTo("PENDING");
		assertThat(response.requestedAt()).isNotNull();
		assertThat(saved.getUserId()).isEqualTo(requester.getUserId());
		assertThat(saved.getStatus()).isEqualTo(ProjectJoinRequestStatus.PENDING);
		assertThat(saved.getSettingSource()).isEqualTo(ProjectJoinSettingSource.PROJECT_CUSTOM);
		assertThat(saved.getRole()).isEqualTo(Role.ETC);
		assertThat(saved.getDetailRole()).isEqualTo("백엔드 개발");
		assertThat(perspectiveRepository.findAllByJoinRequestIdOrderByIdAsc(saved.getId()))
				.extracting(item -> item.getPerspective())
				.containsExactly(Perspective.TECH_RISK, Perspective.ACTION_ITEM);
		assertThat(projectMemberRepository.findByProjectIdAndUserId(project.getId(), requester.getUserId()))
				.isEmpty();
	}

	@Test
	void path_프로젝트와_초대_토큰의_프로젝트가_다르면_거부한다() {
		User owner = saveUser("mismatch-owner@synq.com");
		User requester = saveUser("mismatch-user@synq.com");
		Project pathProject = saveProjectWithOwner(owner, UUID.randomUUID().toString(), LocalDateTime.now().plusDays(7));
		String otherToken = UUID.randomUUID().toString();
		saveProjectWithOwner(owner, otherToken, LocalDateTime.now().plusDays(7));

		assertCode(
				() -> projectService.createJoinRequest(pathProject.getId(), requester.getUserId(), request(otherToken)),
				ProjectErrorCode.INVITATION_PROJECT_MISMATCH
		);
	}

	@Test
	void 만료된_초대_링크는_거부한다() {
		User owner = saveUser("expired-request-owner@synq.com");
		User requester = saveUser("expired-request-user@synq.com");
		String token = UUID.randomUUID().toString();
		Project project = saveProjectWithOwner(owner, token, LocalDateTime.now().minusSeconds(1));

		assertCode(
				() -> projectService.createJoinRequest(project.getId(), requester.getUserId(), request(token)),
				ProjectErrorCode.INVITATION_EXPIRED
		);
	}

	@Test
	void 존재하지_않는_초대_정보는_거부한다() {
		User owner = saveUser("missing-invitation-owner@synq.com");
		User requester = saveUser("missing-invitation-user@synq.com");
		Project project = saveProjectWithOwner(
				owner,
				UUID.randomUUID().toString(),
				LocalDateTime.now().plusDays(7)
		);

		assertCode(
				() -> projectService.createJoinRequest(
						project.getId(),
						requester.getUserId(),
						request(UUID.randomUUID().toString())
				),
				ProjectErrorCode.INVITATION_NOT_FOUND
		);
	}

	@Test
	void 기존_프로젝트_멤버는_참여_요청을_생성할_수_없다() {
		User owner = saveUser("member-request-owner@synq.com");
		String token = UUID.randomUUID().toString();
		Project project = saveProjectWithOwner(owner, token, LocalDateTime.now().plusDays(7));

		assertCode(
				() -> projectService.createJoinRequest(project.getId(), owner.getUserId(), request(token)),
				ProjectErrorCode.ALREADY_PROJECT_MEMBER
		);
	}

	@Test
	void 동일_사용자의_PENDING_요청은_중복_생성할_수_없다() {
		User owner = saveUser("pending-owner@synq.com");
		User requester = saveUser("pending-user@synq.com");
		String token = UUID.randomUUID().toString();
		Project project = saveProjectWithOwner(owner, token, LocalDateTime.now().plusDays(7));
		projectService.createJoinRequest(project.getId(), requester.getUserId(), request(token));

		assertCode(
				() -> projectService.createJoinRequest(project.getId(), requester.getUserId(), request(token)),
				ProjectErrorCode.JOIN_REQUEST_ALREADY_EXISTS
		);
	}

	@ParameterizedTest
	@ValueSource(strings = {"APPROVED", "REJECTED"})
	void 과거_처리된_요청만_있으면_새_PENDING_요청을_생성한다(String processedStatus) {
		User owner = saveUser("history-owner@synq.com");
		User requester = saveUser("history-user@synq.com");
		String token = UUID.randomUUID().toString();
		Project project = saveProjectWithOwner(owner, token, LocalDateTime.now().plusDays(7));
		ProjectJoinRequestCreateResponse first =
				projectService.createJoinRequest(project.getId(), requester.getUserId(), request(token));
		jdbcTemplate.update("UPDATE project_join_request SET status = ? WHERE id = ?", processedStatus, first.requestId());

		ProjectJoinRequestCreateResponse second =
				projectService.createJoinRequest(project.getId(), requester.getUserId(), request(token));

		assertThat(second.requestId()).isNotEqualTo(first.requestId());
		assertThat(participationRequestRepository
				.findAllByProjectIdAndUserIdOrderByRequestedAtAsc(project.getId(), requester.getUserId()))
				.hasSize(2);
	}

	@Test
	void 프로젝트가_OWNER_포함_10명이면_참여_요청을_거부한다() {
		User owner = saveUser("full-request-owner@synq.com");
		String token = UUID.randomUUID().toString();
		Project project = saveProjectWithOwner(owner, token, LocalDateTime.now().plusDays(7));
		for (int index = 0; index < 9; index++) {
			User member = saveUser("full-request-member-%d@synq.com".formatted(index));
			projectMemberRepository.save(ProjectMember.of(project.getId(), member.getUserId(), ProjectMemberRole.MEMBER));
		}
		User requester = saveUser("full-request-user@synq.com");

		assertCode(
				() -> projectService.createJoinRequest(project.getId(), requester.getUserId(), request(token)),
				ProjectErrorCode.PROJECT_MEMBER_LIMIT_EXCEEDED
		);
	}

	@Test
	void ETC_역할에_세부_역할이_없으면_거부한다() {
		assertInvalidRoleSetting(Role.ETC, null, List.of());
	}

	@Test
	void 세부_역할이_30자를_초과하면_거부한다() {
		assertInvalidRoleSetting(Role.DEV_TECH, "가".repeat(31), List.of());
	}

	@Test
	void 관점이_3개를_초과하면_거부한다() {
		assertInvalidRoleSetting(Role.DEV_TECH, null, List.of(
				Perspective.SCHEDULE,
				Perspective.SCOPE,
				Perspective.DECISION,
				Perspective.UX
		));
	}

	@Test
	void 중복_관점은_거부한다() {
		assertInvalidRoleSetting(Role.DEV_TECH, null, List.of(Perspective.UX, Perspective.UX));
	}

	@Test
	void 존재하지_않는_프로젝트는_거부한다() {
		User requester = saveUser("missing-project-requester@synq.com");

		assertCode(
				() -> projectService.createJoinRequest(999999L, requester.getUserId(), request(UUID.randomUUID().toString())),
				ProjectErrorCode.PROJECT_NOT_FOUND
		);
	}

	@Test
	void 존재하지_않는_사용자는_거부한다() {
		User owner = saveUser("missing-user-owner@synq.com");
		String token = UUID.randomUUID().toString();
		Project project = saveProjectWithOwner(owner, token, LocalDateTime.now().plusDays(7));

		assertCode(
				() -> projectService.createJoinRequest(project.getId(), 999999L, request(token)),
				ProjectErrorCode.USER_NOT_FOUND
		);
	}

	private void assertInvalidRoleSetting(Role role, String detailRole, List<Perspective> perspectives) {
		User owner = saveUser(UUID.randomUUID() + "-validation-owner@synq.com");
		User requester = saveUser(UUID.randomUUID() + "-validation-user@synq.com");
		String token = UUID.randomUUID().toString();
		Project project = saveProjectWithOwner(owner, token, LocalDateTime.now().plusDays(7));

		assertCode(
				() -> projectService.createJoinRequest(
						project.getId(),
						requester.getUserId(),
						request(token, ProjectJoinSettingSource.DEFAULT, role, detailRole, perspectives)
				),
				ProjectErrorCode.INVALID_JOIN_REQUEST_ROLE_SETTING
		);
	}

	private ProjectJoinRequestCreateRequest request(String token) {
		return request(token, ProjectJoinSettingSource.DEFAULT, Role.DEV_TECH, null, List.of(Perspective.TECH_RISK));
	}

	private ProjectJoinRequestCreateRequest request(
			String token,
			ProjectJoinSettingSource source,
			Role role,
			String detailRole,
			List<Perspective> perspectives
	) {
		return new ProjectJoinRequestCreateRequest(token, source, role, detailRole, perspectives);
	}

	private Project saveProjectWithOwner(User owner, String token, LocalDateTime expiresAt) {
		Project project = Project.of(owner.getUserId(), "SynQ", "회의 협업 프로젝트");
		project.updateInvitation(token, expiresAt);
		projectRepository.save(project);
		projectMemberRepository.save(ProjectMember.of(project.getId(), owner.getUserId(), ProjectMemberRole.OWNER));
		return project;
	}

	private User saveUser(String email) {
		return userRepository.save(User.ofLocal("테스트", email, "password-hash"));
	}

	private void assertCode(Runnable action, ProjectErrorCode code) {
		assertThatThrownBy(action::run)
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(code));
	}
}
