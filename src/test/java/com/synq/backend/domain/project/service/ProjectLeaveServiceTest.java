package com.synq.backend.domain.project.service;

import com.synq.backend.domain.project.code.ProjectErrorCode;
import com.synq.backend.domain.project.dto.ProjectMemberListResponse;
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
class ProjectLeaveServiceTest extends PostgresTestContainer {

	@Autowired
	private ProjectService projectService;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void MEMBER가_나가면_본인_멤버십만_삭제되고_목록과_인원에_반영된다() {
		User owner = saveUser("leave-owner@synq.com");
		User member = saveUser("leave-member@synq.com");
		User remainingMember = saveUser("leave-remaining@synq.com");
		User otherOwner = saveUser("leave-other-owner@synq.com");
		Project project = saveProject(owner, "나갈 프로젝트");
		Project otherProject = saveProject(otherOwner, "유지할 프로젝트");
		ProjectMember ownerMembership = saveMember(project, owner, ProjectMemberRole.OWNER);
		ProjectMember memberMembership = saveMember(project, member, ProjectMemberRole.MEMBER);
		ProjectMember remainingMembership = saveMember(project, remainingMember, ProjectMemberRole.MEMBER);
		ProjectMember otherOwnerMembership = saveMember(otherProject, otherOwner, ProjectMemberRole.OWNER);
		ProjectMember otherMembership = saveMember(otherProject, member, ProjectMemberRole.MEMBER);

		projectService.leave(project.getId(), member.getUserId());
		ProjectMemberListResponse members = projectService.findMembers(project.getId(), owner.getUserId());

		assertThat(projectMemberRepository.findById(memberMembership.getId())).isEmpty();
		assertThat(projectMemberRepository.findById(ownerMembership.getId())).isPresent();
		assertThat(projectMemberRepository.findById(remainingMembership.getId())).isPresent();
		assertThat(projectMemberRepository.findById(otherOwnerMembership.getId())).isPresent();
		assertThat(projectMemberRepository.findById(otherMembership.getId())).isPresent();
		assertThat(projectMemberRepository.countByProjectId(project.getId())).isEqualTo(2);
		assertThat(members.currentMemberCount()).isEqualTo(2);
		assertThat(members.members())
				.extracting(response -> response.memberId())
				.containsExactly(ownerMembership.getId(), remainingMembership.getId());
		assertThat(projectService.findAll(member.getUserId()))
				.extracting(response -> response.projectId())
				.containsExactly(otherProject.getId())
				.doesNotContain(project.getId());
	}

	@Test
	void 프로젝트_OWNER는_프로젝트에서_나갈_수_없다() {
		User owner = saveUser("leave-owner-forbidden@synq.com");
		Project project = saveProject(owner, "OWNER 프로젝트");
		ProjectMember ownerMembership = saveMember(project, owner, ProjectMemberRole.OWNER);

		assertThatThrownBy(() -> projectService.leave(project.getId(), owner.getUserId()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.PROJECT_OWNER_CANNOT_LEAVE));
		assertThat(projectMemberRepository.findById(ownerMembership.getId())).isPresent();
	}

	@Test
	void 프로젝트_외부_사용자와_이미_나간_사용자는_403이다() {
		User owner = saveUser("leave-non-member-owner@synq.com");
		User member = saveUser("leave-already-member@synq.com");
		User outsider = saveUser("leave-outsider@synq.com");
		Project project = saveProject(owner, "프로젝트");
		ProjectMember ownerMembership = saveMember(project, owner, ProjectMemberRole.OWNER);
		ProjectMember memberMembership = saveMember(project, member, ProjectMemberRole.MEMBER);

		assertNotProjectMember(project.getId(), outsider.getUserId());
		assertThat(projectMemberRepository.findById(ownerMembership.getId())).isPresent();
		assertThat(projectMemberRepository.findById(memberMembership.getId())).isPresent();

		projectService.leave(project.getId(), member.getUserId());
		assertNotProjectMember(project.getId(), member.getUserId());
		assertThat(projectMemberRepository.findById(ownerMembership.getId())).isPresent();
	}

	@Test
	void 존재하지_않거나_삭제된_프로젝트는_404이고_멤버십은_유지된다() {
		User owner = saveUser("leave-project-not-found-owner@synq.com");
		User member = saveUser("leave-project-not-found-member@synq.com");
		Project project = saveProject(owner, "삭제 프로젝트");
		saveMember(project, owner, ProjectMemberRole.OWNER);
		ProjectMember memberMembership = saveMember(project, member, ProjectMemberRole.MEMBER);
		project.softDelete();
		projectRepository.flush();

		assertProjectNotFound(project.getId(), member.getUserId());
		assertProjectNotFound(Long.MAX_VALUE, member.getUserId());
		assertThat(projectMemberRepository.findById(memberMembership.getId())).isPresent();
	}

	@Test
	void 존재하지_않는_사용자는_기존_USER_NOT_FOUND를_발생시킨다() {
		assertThatThrownBy(() -> projectService.leave(1L, Long.MAX_VALUE))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.USER_NOT_FOUND));
	}

	private void assertNotProjectMember(Long projectId, Long userId) {
		assertThatThrownBy(() -> projectService.leave(projectId, userId))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.NOT_PROJECT_MEMBER));
	}

	private void assertProjectNotFound(Long projectId, Long userId) {
		assertThatThrownBy(() -> projectService.leave(projectId, userId))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.PROJECT_NOT_FOUND));
	}

	private Project saveProject(User owner, String title) {
		return projectRepository.save(Project.of(owner.getUserId(), title, null));
	}

	private ProjectMember saveMember(Project project, User user, ProjectMemberRole role) {
		return projectMemberRepository.save(ProjectMember.of(project.getId(), user.getUserId(), role));
	}

	private User saveUser(String email) {
		return userRepository.save(User.ofLocal("테스트", email, "password-hash"));
	}
}
