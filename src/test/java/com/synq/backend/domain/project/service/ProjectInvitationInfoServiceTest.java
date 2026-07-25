package com.synq.backend.domain.project.service;

import com.synq.backend.domain.project.code.ProjectErrorCode;
import com.synq.backend.domain.project.dto.ProjectInvitationInfoResponse;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.project.entity.ProjectMemberRole;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
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

	@Test
	void 유효한_토큰으로_초대_정보를_조회한다() {
		User owner = saveUser("info-owner@synq.com");
		String inviteToken = UUID.randomUUID().toString();
		LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
		Project project = saveProject(owner, inviteToken, expiresAt);
		saveMember(project, owner, ProjectMemberRole.OWNER);

		ProjectInvitationInfoResponse response = projectService.findInvitationInfo(inviteToken, null);

		assertThat(response.projectId()).isEqualTo(project.getId());
		assertThat(response.title()).isEqualTo("SynQ");
		assertThat(response.description()).isEqualTo("AI 회의 협업 프로젝트");
		assertThat(response.currentMemberCount()).isEqualTo(1);
		assertThat(response.maxMemberCount()).isEqualTo(10);
		assertThat(response.alreadyJoined()).isFalse();
		assertThat(response.expiresAt()).isEqualTo(expiresAt);
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

	private void saveMember(Project project, User user, ProjectMemberRole role) {
		projectMemberRepository.save(ProjectMember.of(project.getId(), user.getUserId(), role));
	}

	private User saveUser(String email) {
		return userRepository.save(User.ofLocal("테스트", email, "password-hash"));
	}
}
