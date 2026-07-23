package com.synq.backend.domain.project.service;

import com.synq.backend.domain.project.code.ProjectErrorCode;
import com.synq.backend.domain.project.dto.ProjectCreateRequest;
import com.synq.backend.domain.project.dto.ProjectCreateResponse;
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
class ProjectServiceTest extends PostgresTestContainer {

	@Autowired
	private ProjectService projectService;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void 프로젝트를_생성하고_생성자를_OWNER로_등록한다() {
		User owner = saveUser("owner-create@synq.com");

		ProjectCreateResponse response = projectService.create(
				owner.getUserId(), new ProjectCreateRequest("SynQ", "회의 협업 프로젝트"));

		Project project = projectRepository.findById(response.projectId()).orElseThrow();
		ProjectMember member = projectMemberRepository
				.findByProjectIdAndUserId(project.getId(), owner.getUserId()).orElseThrow();
		assertThat(response.ownerId()).isEqualTo(owner.getUserId());
		assertThat(response.title()).isEqualTo("SynQ");
		assertThat(response.createdAt()).isNotNull();
		assertThat(member.getRole()).isEqualTo(ProjectMemberRole.OWNER);
	}

	@Test
	void 사용자가_20개_프로젝트에_참여했으면_새_프로젝트_생성을_거부한다() {
		User user = saveUser("user-limit@synq.com");
		for (int index = 0; index < 20; index++) {
			Project project = projectRepository.save(Project.of(user.getUserId(), "프로젝트%d".formatted(index), null));
			projectMemberRepository.save(ProjectMember.of(project.getId(), user.getUserId(), ProjectMemberRole.MEMBER));
		}

		assertThatThrownBy(() -> projectService.create(user.getUserId(), new ProjectCreateRequest("초과", null)))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.USER_PROJECT_LIMIT_EXCEEDED));
	}

	private User saveUser(String email) {
		return userRepository.save(User.ofLocal("테스트", email, "password-hash"));
	}
}
