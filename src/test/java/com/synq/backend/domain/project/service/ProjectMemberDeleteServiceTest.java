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
class ProjectMemberDeleteServiceTest extends PostgresTestContainer {

	@Autowired
	private ProjectService projectService;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void OWNER가_일반_멤버를_삭제한다() {
		User owner = saveUser("member-delete-owner@synq.com");
		User member = saveUser("member-delete-member@synq.com");
		User remainingMember = saveUser("member-delete-remaining-member@synq.com");
		Project project = saveProject(owner);
		ProjectMember ownerMembership = saveMember(project, owner, ProjectMemberRole.OWNER);
		ProjectMember memberMembership = saveMember(project, member, ProjectMemberRole.MEMBER);
		ProjectMember remainingMembership = saveMember(project, remainingMember, ProjectMemberRole.MEMBER);

		projectService.deleteMember(project.getId(), memberMembership.getId(), owner.getUserId());
		ProjectMemberListResponse memberList = projectService.findMembers(project.getId(), owner.getUserId());

		assertThat(projectMemberRepository.findById(memberMembership.getId())).isEmpty();
		assertThat(projectMemberRepository.findById(ownerMembership.getId())).isPresent();
		assertThat(projectMemberRepository.findById(remainingMembership.getId())).isPresent();
		assertThat(projectMemberRepository.countByProjectId(project.getId())).isEqualTo(2L);
		assertThat(memberList.currentMemberCount()).isEqualTo(2);
		assertThat(memberList.members())
				.extracting(response -> response.memberId())
				.containsExactly(ownerMembership.getId(), remainingMembership.getId());
	}

	@Test
	void 프로젝트_OWNER는_삭제할_수_없다() {
		User owner = saveUser("member-delete-self-owner@synq.com");
		Project project = saveProject(owner);
		ProjectMember ownerMembership = saveMember(project, owner, ProjectMemberRole.OWNER);

		assertThatThrownBy(() -> projectService.deleteMember(
				project.getId(), ownerMembership.getId(), owner.getUserId()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.CANNOT_DELETE_PROJECT_OWNER));
		assertThat(projectMemberRepository.findById(ownerMembership.getId())).isPresent();
	}

	@Test
	void OWNER가_아니면_멤버를_삭제할_수_없다() {
		User owner = saveUser("member-delete-forbidden-owner@synq.com");
		User member = saveUser("member-delete-forbidden-member@synq.com");
		User target = saveUser("member-delete-forbidden-target@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(project, member, ProjectMemberRole.MEMBER);
		ProjectMember targetMembership = saveMember(project, target, ProjectMemberRole.MEMBER);

		assertThatThrownBy(() -> projectService.deleteMember(
				project.getId(), targetMembership.getId(), member.getUserId()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.NOT_PROJECT_OWNER));
		assertThat(projectMemberRepository.findById(targetMembership.getId())).isPresent();
	}

	@Test
	void 다른_프로젝트의_멤버_ID는_찾을_수_없다() {
		User owner = saveUser("member-delete-other-owner@synq.com");
		User otherOwner = saveUser("member-delete-other-project-owner@synq.com");
		User member = saveUser("member-delete-other-member@synq.com");
		Project project = saveProject(owner);
		Project otherProject = saveProject(otherOwner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(otherProject, otherOwner, ProjectMemberRole.OWNER);
		ProjectMember otherMembership = saveMember(otherProject, member, ProjectMemberRole.MEMBER);

		assertThatThrownBy(() -> projectService.deleteMember(
				project.getId(), otherMembership.getId(), owner.getUserId()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND));
		assertThat(projectMemberRepository.findById(otherMembership.getId())).isPresent();
	}

	@Test
	void 존재하지_않거나_이미_삭제된_멤버는_다시_삭제할_수_없다() {
		User owner = saveUser("member-delete-missing-member-owner@synq.com");
		User member = saveUser("member-delete-missing-member@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		ProjectMember memberMembership = saveMember(project, member, ProjectMemberRole.MEMBER);

		assertThatThrownBy(() -> projectService.deleteMember(
				project.getId(), Long.MAX_VALUE, owner.getUserId()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND));
		assertThat(projectMemberRepository.findById(memberMembership.getId())).isPresent();

		projectService.deleteMember(project.getId(), memberMembership.getId(), owner.getUserId());
		assertThatThrownBy(() -> projectService.deleteMember(
				project.getId(), memberMembership.getId(), owner.getUserId()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND));
	}

	@Test
	void 존재하지_않거나_삭제된_프로젝트는_404를_발생시킨다() {
		User owner = saveUser("member-delete-missing-project-owner@synq.com");
		Project project = saveProject(owner);
		ProjectMember ownerMembership = saveMember(project, owner, ProjectMemberRole.OWNER);
		project.softDelete();
		projectRepository.flush();

		assertThatThrownBy(() -> projectService.deleteMember(
				project.getId(), ownerMembership.getId(), owner.getUserId()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.PROJECT_NOT_FOUND));
		assertThatThrownBy(() -> projectService.deleteMember(
				Long.MAX_VALUE, ownerMembership.getId(), owner.getUserId()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.PROJECT_NOT_FOUND));
	}

	@Test
	void 존재하지_않는_사용자면_기존_USER_NOT_FOUND를_발생시킨다() {
		assertThatThrownBy(() -> projectService.deleteMember(1L, 1L, Long.MAX_VALUE))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.USER_NOT_FOUND));
	}

	private Project saveProject(User owner) {
		return projectRepository.save(Project.of(owner.getUserId(), "SynQ", null));
	}

	private ProjectMember saveMember(Project project, User user, ProjectMemberRole role) {
		return projectMemberRepository.save(ProjectMember.of(project.getId(), user.getUserId(), role));
	}

	private User saveUser(String email) {
		return userRepository.save(User.ofLocal("테스트", email, "password-hash"));
	}
}
