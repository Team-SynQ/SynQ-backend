package com.synq.backend.domain.project.repository;

import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.project.entity.ProjectMemberRole;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ProjectRepositoryTest extends PostgresTestContainer {

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void 프로젝트별_인원과_사용자별_프로젝트_수를_조회한다() {
		User owner = saveUser("owner-count@synq.com");
		Project project = projectRepository.save(Project.of(owner.getUserId(), "SynQ", null));
		projectMemberRepository.save(ProjectMember.of(project.getId(), owner.getUserId(), ProjectMemberRole.OWNER));

		assertThat(projectMemberRepository.countByUserId(owner.getUserId())).isEqualTo(1);
	}

	private User saveUser(String email) {
		return userRepository.save(User.ofLocal("테스트", email, "password-hash"));
	}
}
