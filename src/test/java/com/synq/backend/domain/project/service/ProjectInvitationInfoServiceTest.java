package com.synq.backend.domain.project.service;

import com.synq.backend.domain.project.code.ProjectErrorCode;
import com.synq.backend.domain.project.dto.ProjectInvitationInfoResponse;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.project.entity.ProjectMemberRole;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.entity.Role;
import com.synq.backend.domain.user.entity.RoleProfile;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.domain.user.repository.RoleProfileRepository;
import com.synq.backend.global.apipayload.exception.GeneralException;
import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class ProjectInvitationInfoServiceTest extends PostgresTestContainer {

	@Autowired
	private ProjectService projectService;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleProfileRepository roleProfileRepository;

	@Test
	void 유효한_토큰으로_초대_정보를_조회한다() {
		User owner = saveUser("박서은", "info-owner@synq.com");
		owner.updateProfileImageKey("profiles/owner.png");
		String inviteToken = UUID.randomUUID().toString();
		LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
		Project project = saveProject(owner, inviteToken, expiresAt);
		ProjectMember ownerMember = saveMember(project, owner, ProjectMemberRole.OWNER);
		ownerMember.updateRolePerspective(true, Role.PLANNING_OPERATION, "기획자");

		ProjectInvitationInfoResponse response = projectService.findInvitationInfo(inviteToken, null);

		assertThat(response.projectId()).isEqualTo(project.getId());
		assertThat(response.title()).isEqualTo("SynQ");
		assertThat(response.description()).isEqualTo("AI 회의 협업 프로젝트");
		assertThat(response.currentMemberCount()).isEqualTo(1);
		assertThat(response.maxMemberCount()).isEqualTo(10);
		assertThat(response.alreadyJoined()).isFalse();
		assertThat(response.expiresAt()).isEqualTo(expiresAt);
		assertThat(response.owner().userId()).isEqualTo(owner.getUserId());
		assertThat(response.owner().name()).isEqualTo("박서은");
		assertThat(response.owner().profileImageUrl())
				.isEqualTo("http://localhost-cloudfront-not-configured/profiles/owner.png");
		assertThat(response.owner().roleCategory()).isEqualTo(Role.PLANNING_OPERATION);
	}

	@Test
	void 저장된_프로젝트_역할이_없는_기존_OWNER는_기본_프로필_역할을_반환한다() {
		User owner = saveUser("기존 소유자", "info-owner-fallback@synq.com");
		RoleProfile defaultProfile = roleProfileRepository.save(
				RoleProfile.of(owner.getUserId(), Role.DESIGN_CONTENT, "디자이너", true));
		String inviteToken = UUID.randomUUID().toString();
		Project project = saveProject(owner, inviteToken, LocalDateTime.now().plusDays(7));
		saveMember(project, owner, ProjectMemberRole.OWNER);

		ProjectInvitationInfoResponse response = projectService.findInvitationInfo(inviteToken, null);

		assertThat(defaultProfile.isDefault()).isTrue();
		assertThat(response.owner().roleCategory()).isEqualTo(Role.DESIGN_CONTENT);
	}

	@Test
	void OWNER의_프로필_이미지와_역할이_없으면_null을_반환한다() {
		User owner = saveUser("정보 없는 소유자", "info-owner-null@synq.com");
		String inviteToken = UUID.randomUUID().toString();
		Project project = saveProject(owner, inviteToken, LocalDateTime.now().plusDays(7));
		saveMember(project, owner, ProjectMemberRole.OWNER);

		ProjectInvitationInfoResponse response = projectService.findInvitationInfo(inviteToken, null);

		assertThat(response.owner().profileImageUrl()).isNull();
		assertThat(response.owner().roleCategory()).isNull();
	}

	@Test
	void 로그인_사용자가_기존_멤버이면_alreadyJoined가_true이다() {
		User owner = saveUser("joined-owner@synq.com");
		User member = saveUser("joined-member@synq.com");
		String inviteToken = UUID.randomUUID().toString();
		Project project = saveProject(owner, inviteToken, LocalDateTime.now().plusDays(7));
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(project, member, ProjectMemberRole.MEMBER);

		ProjectInvitationInfoResponse response = projectService.findInvitationInfo(
				inviteToken, member.getUserId());

		assertThat(response.alreadyJoined()).isTrue();
	}

	@Test
	void 로그인_사용자가_프로젝트_멤버가_아니면_alreadyJoined가_false이다() {
		User owner = saveUser("not-joined-owner@synq.com");
		User outsider = saveUser("not-joined-user@synq.com");
		String inviteToken = UUID.randomUUID().toString();
		Project project = saveProject(owner, inviteToken, LocalDateTime.now().plusDays(7));
		saveMember(project, owner, ProjectMemberRole.OWNER);

		ProjectInvitationInfoResponse response = projectService.findInvitationInfo(
				inviteToken, outsider.getUserId());

		assertThat(response.alreadyJoined()).isFalse();
	}

	@Test
	void 비로그인_사용자의_alreadyJoined는_false이다() {
		User owner = saveUser("anonymous-owner@synq.com");
		String inviteToken = UUID.randomUUID().toString();
		Project project = saveProject(owner, inviteToken, LocalDateTime.now().plusDays(7));
		saveMember(project, owner, ProjectMemberRole.OWNER);

		ProjectInvitationInfoResponse response = projectService.findInvitationInfo(inviteToken, null);

		assertThat(response.alreadyJoined()).isFalse();
	}

	@Test
	void 존재하지_않는_토큰이면_404_예외를_던진다() {
		assertThatThrownBy(() -> projectService.findInvitationInfo(UUID.randomUUID().toString(), null))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.INVITATION_NOT_FOUND));
	}

	@Test
	void 만료된_토큰이면_410_예외를_던진다() {
		User owner = saveUser("expired-info-owner@synq.com");
		String inviteToken = UUID.randomUUID().toString();
		saveProject(owner, inviteToken, LocalDateTime.now().minusSeconds(1));

		assertThatThrownBy(() -> projectService.findInvitationInfo(inviteToken, null))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.INVITATION_EXPIRED));
	}

	@Test
	void 프로젝트가_최대_인원_10명이어도_초대_정보를_조회한다() {
		User owner = saveUser("full-info-owner@synq.com");
		String inviteToken = UUID.randomUUID().toString();
		Project project = saveProject(owner, inviteToken, LocalDateTime.now().plusDays(7));
		saveMember(project, owner, ProjectMemberRole.OWNER);
		for (int index = 0; index < 9; index++) {
			User member = saveUser("full-info-member-%d@synq.com".formatted(index));
			saveMember(project, member, ProjectMemberRole.MEMBER);
		}

		ProjectInvitationInfoResponse response = projectService.findInvitationInfo(inviteToken, null);

		assertThat(response.currentMemberCount()).isEqualTo(10);
		assertThat(response.maxMemberCount()).isEqualTo(10);
	}

	@Test
	void 현재_프로젝트_멤버_수를_정확히_반환한다() {
		User owner = saveUser("count-info-owner@synq.com");
		String inviteToken = UUID.randomUUID().toString();
		Project project = saveProject(owner, inviteToken, LocalDateTime.now().plusDays(7));
		saveMember(project, owner, ProjectMemberRole.OWNER);
		for (int index = 0; index < 4; index++) {
			User member = saveUser("count-info-member-%d@synq.com".formatted(index));
			saveMember(project, member, ProjectMemberRole.MEMBER);
		}

		ProjectInvitationInfoResponse response = projectService.findInvitationInfo(inviteToken, null);

		assertThat(response.currentMemberCount()).isEqualTo(5);
	}

	private Project saveProject(User owner, String inviteToken, LocalDateTime expiresAt) {
		Project project = Project.of(owner.getUserId(), "SynQ", "AI 회의 협업 프로젝트");
		project.updateInvitation(inviteToken, expiresAt);
		return projectRepository.save(project);
	}

	private ProjectMember saveMember(Project project, User user, ProjectMemberRole role) {
		return projectMemberRepository.save(ProjectMember.of(project.getId(), user.getUserId(), role));
	}

	private User saveUser(String email) {
		return saveUser("테스트", email);
	}

	private User saveUser(String name, String email) {
		return userRepository.save(User.ofLocal(name, email, "password-hash"));
	}
}
