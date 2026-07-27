package com.synq.backend.domain.project.service;

import com.synq.backend.domain.project.code.ProjectErrorCode;
import com.synq.backend.domain.project.dto.ProjectDetailResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class ProjectDetailServiceTest extends PostgresTestContainer {

	@Autowired
	private ProjectService projectService;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void OWNER는_프로젝트_상세를_조회할_수_있다() {
		User owner = saveUser("detail-owner@synq.com");
		Project project = saveProject(owner, "SynQ", "회의 협업 프로젝트");
		saveMember(project, owner, ProjectMemberRole.OWNER);

		ProjectDetailResponse response = projectService.findById(project.getId(), owner.getUserId());

		assertThat(response.projectId()).isEqualTo(project.getId());
		assertThat(response.ownerId()).isEqualTo(owner.getUserId());
		assertThat(response.title()).isEqualTo("SynQ");
		assertThat(response.description()).isEqualTo("회의 협업 프로젝트");
		assertThat(response.createdAt()).isEqualTo(project.getCreatedAt());
		assertThat(response.updatedAt()).isEqualTo(project.getUpdatedAt());
	}

	@Test
	void MEMBER는_프로젝트_상세를_조회할_수_있다() {
		User owner = saveUser("detail-member-owner@synq.com");
		User member = saveUser("detail-member@synq.com");
		Project project = saveProject(owner, "SynQ", null);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(project, member, ProjectMemberRole.MEMBER);

		ProjectDetailResponse response = projectService.findById(project.getId(), member.getUserId());

		assertThat(response.projectId()).isEqualTo(project.getId());
		assertThat(response.description()).isNull();
	}

	@Test
	void 프로젝트_외부_사용자는_상세를_조회할_수_없다() {
		User owner = saveUser("detail-outsider-owner@synq.com");
		User outsider = saveUser("detail-outsider@synq.com");
		Project project = saveProject(owner, "SynQ", null);
		saveMember(project, owner, ProjectMemberRole.OWNER);

		assertThatThrownBy(() -> projectService.findById(project.getId(), outsider.getUserId()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.NOT_PROJECT_MEMBER));
	}

	@Test
	void 존재하지_않는_프로젝트는_조회할_수_없다() {
		User user = saveUser("detail-missing@synq.com");

		assertThatThrownBy(() -> projectService.findById(Long.MAX_VALUE, user.getUserId()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.PROJECT_NOT_FOUND));
	}

	private Project saveProject(User owner, String title, String description) {
		return projectRepository.save(Project.of(owner.getUserId(), title, description));
	}

	private void saveMember(Project project, User user, ProjectMemberRole role) {
		projectMemberRepository.save(ProjectMember.of(project.getId(), user.getUserId(), role));
	}

	private User saveUser(String email) {
		return userRepository.save(User.ofLocal("테스트", email, "password-hash"));
	}
}
