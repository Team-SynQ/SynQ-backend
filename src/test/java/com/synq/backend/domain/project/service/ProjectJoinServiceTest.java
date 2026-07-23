package com.synq.backend.domain.project.service;

import com.synq.backend.domain.project.code.ProjectErrorCode;
import com.synq.backend.domain.project.dto.ProjectJoinResponse;
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
class ProjectJoinServiceTest extends PostgresTestContainer {

	@Autowired
	private ProjectService projectService;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void 유효한_초대_토큰으로_MEMBER로_참여한다() {
		User owner = saveUser("join-owner@synq.com");
		User participant = saveUser("join-participant@synq.com");
		String inviteToken = UUID.randomUUID().toString();
		Project project = saveProjectWithOwner(owner, inviteToken, LocalDateTime.now().plusDays(7));

		ProjectJoinResponse response = projectService.join(participant.getUserId(), inviteToken);

		assertThat(response.newlyJoined()).isTrue();
		assertThat(response.projectId()).isEqualTo(project.getId());
		assertThat(response.title()).isEqualTo("SynQ");
		assertThat(response.description()).isEqualTo("회의 협업 프로젝트");
		assertThat(response.memberRole()).isEqualTo("MEMBER");
		assertThat(response.joinedAt()).isNotNull();
		assertThat(projectMemberRepository.findByProjectIdAndUserId(project.getId(), participant.getUserId()))
				.get().extracting(ProjectMember::getRole).isEqualTo(ProjectMemberRole.MEMBER);
	}

	@Test
	void 이미_참여한_사용자는_기존_참여_정보를_반환한다() {
		User owner = saveUser("duplicate-owner@synq.com");
		String inviteToken = UUID.randomUUID().toString();
		Project project = saveProjectWithOwner(owner, inviteToken, LocalDateTime.now().plusDays(7));
		ProjectMember existingMember = projectMemberRepository
				.findByProjectIdAndUserId(project.getId(), owner.getUserId()).orElseThrow();

		ProjectJoinResponse response = projectService.join(owner.getUserId(), inviteToken);

		assertThat(response.newlyJoined()).isFalse();
		assertThat(response.memberRole()).isEqualTo("OWNER");
		assertThat(response.joinedAt()).isEqualTo(existingMember.getJoinedAt());
		assertThat(projectMemberRepository.countByProjectId(project.getId())).isEqualTo(1);
	}

	@Test
	void 존재하지_않는_초대_토큰은_거부한다() {
		User participant = saveUser("not-found-participant@synq.com");

		assertThatThrownBy(() -> projectService.join(participant.getUserId(), UUID.randomUUID().toString()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.INVITATION_NOT_FOUND));
	}

	@Test
	void 만료된_초대_토큰은_거부한다() {
		User owner = saveUser("expired-owner@synq.com");
		User participant = saveUser("expired-participant@synq.com");
		String inviteToken = UUID.randomUUID().toString();
		saveProjectWithOwner(owner, inviteToken, LocalDateTime.now().minusSeconds(1));

		assertThatThrownBy(() -> projectService.join(participant.getUserId(), inviteToken))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.INVITATION_EXPIRED));
	}

	@Test
	void 프로젝트가_OWNER_포함_10명이면_추가_참여를_거부한다() {
		User owner = saveUser("full-owner@synq.com");
		String inviteToken = UUID.randomUUID().toString();
		Project project = saveProjectWithOwner(owner, inviteToken, LocalDateTime.now().plusDays(7));
		for (int index = 0; index < 9; index++) {
			User member = saveUser("full-member-%d@synq.com".formatted(index));
			projectMemberRepository.save(ProjectMember.of(project.getId(), member.getUserId(), ProjectMemberRole.MEMBER));
		}
		User participant = saveUser("full-participant@synq.com");

		assertThatThrownBy(() -> projectService.join(participant.getUserId(), inviteToken))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.PROJECT_MEMBER_LIMIT_EXCEEDED));
	}

	@Test
	void 사용자가_20개_프로젝트에_참여했으면_추가_참여를_거부한다() {
		User target = saveUser("limited-participant@synq.com");
		for (int index = 0; index < 20; index++) {
			Project joinedProject = projectRepository.save(
					Project.of(target.getUserId(), "참여 프로젝트 %d".formatted(index), null));
			projectMemberRepository.save(
					ProjectMember.of(joinedProject.getId(), target.getUserId(), ProjectMemberRole.MEMBER));
		}
		User owner = saveUser("limited-owner@synq.com");
		String inviteToken = UUID.randomUUID().toString();
		saveProjectWithOwner(owner, inviteToken, LocalDateTime.now().plusDays(7));

		assertThatThrownBy(() -> projectService.join(target.getUserId(), inviteToken))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.USER_PROJECT_LIMIT_EXCEEDED));
	}

	private Project saveProjectWithOwner(User owner, String inviteToken, LocalDateTime expiresAt) {
		Project project = Project.of(owner.getUserId(), "SynQ", "회의 협업 프로젝트");
		project.updateInvitation(inviteToken, expiresAt);
		projectRepository.save(project);
		projectMemberRepository.save(ProjectMember.of(project.getId(), owner.getUserId(), ProjectMemberRole.OWNER));
		return project;
	}

	private User saveUser(String email) {
		return userRepository.save(User.ofLocal("테스트", email, "password-hash"));
	}
}
