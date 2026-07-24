package com.synq.backend.domain.project.service;

import com.synq.backend.domain.project.code.ProjectErrorCode;
import com.synq.backend.domain.project.dto.ProjectInvitationResponse;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.global.apipayload.code.GeneralErrorCode;
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
class ProjectInvitationServiceTest extends PostgresTestContainer {

	@Autowired
	private ProjectService projectService;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void 초대_토큰이_없으면_UUID_토큰과_7일_만료_링크를_생성한다() {
		User owner = saveUser("invitation-owner@synq.com");
		Project project = projectRepository.save(Project.of(owner.getUserId(), "SynQ", null));
		LocalDateTime before = LocalDateTime.now();

		ProjectInvitationResponse response = projectService.createInvitation(project.getId(), owner.getUserId());

		Project savedProject = projectRepository.findById(project.getId()).orElseThrow();
		assertThatCodeIsUuid(savedProject.getInviteToken());
		assertThat(savedProject.getInviteTokenExpiresAt())
				.isBetween(before.plusDays(7), LocalDateTime.now().plusDays(7));
		assertThat(response.inviteUrl())
				.isEqualTo("https://synq.app/invite/%s".formatted(savedProject.getInviteToken()));
		assertThat(response.expiresAt()).isEqualTo(savedProject.getInviteTokenExpiresAt());
	}

	@Test
	void 유효한_초대_토큰이_있으면_재발급하지_않고_기존_정보를_반환한다() {
		User owner = saveUser("valid-owner@synq.com");
		String inviteToken = UUID.randomUUID().toString();
		LocalDateTime expiresAt = LocalDateTime.now().plusDays(3);
		Project project = saveProjectWithInvitation(owner, inviteToken, expiresAt);

		ProjectInvitationResponse response = projectService.createInvitation(project.getId(), owner.getUserId());

		Project savedProject = projectRepository.findById(project.getId()).orElseThrow();
		assertThat(savedProject.getInviteToken()).isEqualTo(inviteToken);
		assertThat(savedProject.getInviteTokenExpiresAt()).isEqualTo(expiresAt);
		assertThat(response.inviteUrl()).endsWith("/invite/" + inviteToken);
		assertThat(response.expiresAt()).isEqualTo(expiresAt);
	}

	@Test
	void 만료된_초대_토큰이면_새로운_UUID와_만료시간으로_갱신한다() {
		User owner = saveUser("expired-invitation-owner@synq.com");
		String expiredToken = UUID.randomUUID().toString();
		Project project = saveProjectWithInvitation(owner, expiredToken, LocalDateTime.now().minusSeconds(1));
		LocalDateTime before = LocalDateTime.now();

		ProjectInvitationResponse response = projectService.createInvitation(project.getId(), owner.getUserId());

		Project savedProject = projectRepository.findById(project.getId()).orElseThrow();
		assertThat(savedProject.getInviteToken()).isNotEqualTo(expiredToken);
		assertThatCodeIsUuid(savedProject.getInviteToken());
		assertThat(savedProject.getInviteTokenExpiresAt())
				.isBetween(before.plusDays(7), LocalDateTime.now().plusDays(7));
		assertThat(response.inviteUrl()).endsWith("/invite/" + savedProject.getInviteToken());
	}

	@Test
	void 인증되지_않은_사용자는_초대_링크를_생성할_수_없다() {
		assertThatThrownBy(() -> projectService.createInvitation(1L, null))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(GeneralErrorCode.UNAUTHORIZED));
	}

	@Test
	void 존재하지_않는_사용자는_초대_링크를_생성할_수_없다() {
		assertThatThrownBy(() -> projectService.createInvitation(1L, Long.MAX_VALUE))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(ProjectErrorCode.USER_NOT_FOUND));
	}

	@Test
	void 존재하지_않는_프로젝트는_초대_링크를_생성할_수_없다() {
		User owner = saveUser("missing-project-owner@synq.com");

		assertThatThrownBy(() -> projectService.createInvitation(Long.MAX_VALUE, owner.getUserId()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(ProjectErrorCode.PROJECT_NOT_FOUND));
	}

	@Test
	void 프로젝트_소유자가_아니면_초대_링크를_생성할_수_없다() {
		User owner = saveUser("permission-owner@synq.com");
		User member = saveUser("permission-member@synq.com");
		Project project = projectRepository.save(Project.of(owner.getUserId(), "SynQ", null));

		assertThatThrownBy(() -> projectService.createInvitation(project.getId(), member.getUserId()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(ProjectErrorCode.NOT_PROJECT_OWNER));
	}

	private Project saveProjectWithInvitation(User owner, String inviteToken, LocalDateTime expiresAt) {
		Project project = Project.of(owner.getUserId(), "SynQ", null);
		project.updateInvitation(inviteToken, expiresAt);
		return projectRepository.save(project);
	}

	private User saveUser(String email) {
		return userRepository.save(User.ofLocal("테스트", email, "password-hash"));
	}

	private void assertThatCodeIsUuid(String inviteToken) {
		assertThat(UUID.fromString(inviteToken).toString()).isEqualTo(inviteToken);
	}
}
