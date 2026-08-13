package com.synq.backend.domain.project.service;

import com.synq.backend.domain.project.code.ProjectErrorCode;
import com.synq.backend.domain.project.dto.ProjectUpdateRequest;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.project.entity.ProjectMemberRole;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.global.apipayload.exception.GeneralException;
import com.synq.backend.support.PostgresTestContainer;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class ProjectDeleteServiceTest extends PostgresTestContainer {

	@Autowired
	private ProjectService projectService;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManager entityManager;

	@Test
	void OWNER가_프로젝트를_Soft_Delete한다() {
		User owner = saveUser("delete-owner@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);

		projectService.delete(project.getId(), owner.getUserId());

		Boolean deleted = jdbcTemplate.queryForObject(
				"SELECT deleted_at IS NOT NULL FROM project WHERE id = ?",
				Boolean.class,
				project.getId()
		);
		Long projectCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM project WHERE id = ?",
				Long.class,
				project.getId()
		);
		Long memberCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM project_member WHERE project_id = ?",
				Long.class,
				project.getId()
		);
		assertThat(deleted).isTrue();
		assertThat(projectCount).isEqualTo(1L);
		assertThat(memberCount).isEqualTo(1L);

		entityManager.clear();
		assertThat(projectRepository.findById(project.getId())).isEmpty();
	}

	@Test
	void 삭제된_프로젝트는_상세_목록_수정에서_접근할_수_없다() {
		User owner = saveUser("delete-access-owner@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		projectService.delete(project.getId(), owner.getUserId());
		ProjectUpdateRequest updateRequest = new ProjectUpdateRequest();
		updateRequest.setTitle("수정 불가");

		assertThatThrownBy(() -> projectService.findById(project.getId(), owner.getUserId()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.PROJECT_NOT_FOUND));
		assertThat(projectService.findAll(owner.getUserId())).isEmpty();
		assertThatThrownBy(() -> projectService.update(
				project.getId(), owner.getUserId(), updateRequest))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.PROJECT_NOT_FOUND));
	}

	@Test
	void 삭제된_프로젝트의_초대_정보를_차단한다() {
		User owner = saveUser("delete-invitation-owner@synq.com");
		User member = saveUser("delete-invitation-member@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		project.updateInvitation("delete-invitation-token", java.time.LocalDateTime.now().plusDays(7));
		projectRepository.flush();
		projectService.delete(project.getId(), owner.getUserId());

		assertThatThrownBy(() -> projectService.findInvitationInfo("delete-invitation-token", member.getUserId()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.INVITATION_NOT_FOUND));
	}

	@Test
	void 삭제된_프로젝트의_멤버십은_존재하지_않는_것으로_처리한다() {
		User owner = saveUser("delete-membership-owner@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		projectService.delete(project.getId(), owner.getUserId());

		ProjectMembershipCheckerImpl checker = new ProjectMembershipCheckerImpl(projectMemberRepository);

		assertThat(projectMemberRepository.findByProjectIdAndUserId(
				project.getId(), owner.getUserId())).isEmpty();
		assertThat(checker.isMember(project.getId(), owner.getUserId())).isFalse();
		assertThat(projectMemberRepository.countByProjectId(project.getId())).isZero();
		assertThat(projectMemberRepository.countByUserId(owner.getUserId())).isZero();
	}

	@Test
	void 삭제된_프로젝트를_다시_삭제하면_404_예외를_발생시킨다() {
		User owner = saveUser("delete-again-owner@synq.com");
		Project project = saveProject(owner);
		projectService.delete(project.getId(), owner.getUserId());

		assertThatThrownBy(() -> projectService.delete(project.getId(), owner.getUserId()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.PROJECT_NOT_FOUND));
	}

	@Test
	void OWNER가_아닌_사용자는_프로젝트를_삭제할_수_없다() {
		User owner = saveUser("delete-forbidden-owner@synq.com");
		User member = saveUser("delete-forbidden-member@synq.com");
		Project project = saveProject(owner);

		assertThatThrownBy(() -> projectService.delete(project.getId(), member.getUserId()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.NOT_PROJECT_OWNER));
	}

	@Test
	void 존재하지_않는_프로젝트면_404_예외를_발생시킨다() {
		User owner = saveUser("delete-missing-project@synq.com");

		assertThatThrownBy(() -> projectService.delete(Long.MAX_VALUE, owner.getUserId()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.PROJECT_NOT_FOUND));
	}

	@Test
	void 존재하지_않는_사용자면_기존_USER_NOT_FOUND_예외를_발생시킨다() {
		assertThatThrownBy(() -> projectService.delete(1L, Long.MAX_VALUE))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.USER_NOT_FOUND));
	}

	private Project saveProject(User owner) {
		return projectRepository.save(Project.of(owner.getUserId(), "SynQ", "프로젝트 설명"));
	}

	private void saveMember(Project project, User user, ProjectMemberRole role) {
		projectMemberRepository.save(ProjectMember.of(project.getId(), user.getUserId(), role));
	}

	private User saveUser(String email) {
		return userRepository.save(User.ofLocal("테스트", email, "password-hash"));
	}
}
