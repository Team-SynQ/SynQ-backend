package com.synq.backend.domain.project.repository;

import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.project.entity.ProjectMemberRole;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.support.PostgresTestContainer;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ProjectRepositoryTest extends PostgresTestContainer {

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void 프로젝트별_인원과_사용자별_프로젝트_수를_조회한다() {
		User owner = saveUser("owner-count@synq.com");
		Project project = projectRepository.save(Project.of(owner.getUserId(), "SynQ", null));
		projectMemberRepository.save(ProjectMember.of(project.getId(), owner.getUserId(), ProjectMemberRole.OWNER));

		assertThat(projectMemberRepository.countByUserId(owner.getUserId())).isEqualTo(1);
	}

	@Test
	void 일반_Project_조회에서_삭제된_프로젝트를_제외한다() {
		User owner = saveUser("soft-delete-repository@synq.com");
		Project activeProject = projectRepository.save(
				Project.of(owner.getUserId(), "활성 프로젝트", null));
		Project deletedProject = projectRepository.save(
				Project.of(owner.getUserId(), "삭제 프로젝트", null));
		deletedProject.updateInvitation("deleted-project-token", LocalDateTime.now().plusDays(7));
		deletedProject.softDelete();
		projectRepository.flush();
		entityManager.clear();

		assertThat(projectRepository.findById(deletedProject.getId())).isEmpty();
		assertThat(projectRepository.findAll())
				.extracting(Project::getId)
				.contains(activeProject.getId())
				.doesNotContain(deletedProject.getId());
		assertThat(projectRepository.findAllById(List.of(activeProject.getId(), deletedProject.getId())))
				.extracting(Project::getId)
				.containsExactly(activeProject.getId());
		assertThat(projectRepository.findByInviteToken("deleted-project-token")).isEmpty();
	}

	private User saveUser(String email) {
		return userRepository.save(User.ofLocal("테스트", email, "password-hash"));
	}
}
