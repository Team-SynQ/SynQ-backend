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
import com.synq.backend.global.apipayload.code.GeneralErrorCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class ProjectMemberListServiceTest extends PostgresTestContainer {

	@Autowired
	private ProjectService projectService;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void OWNER는_OWNER를_포함한_멤버_목록을_참여일_오름차순으로_조회한다() {
		User owner = saveUser("소유자", "member-list-owner@synq.com");
		User member = saveUser("멤버", "member-list-member@synq.com");
		Project project = saveProject(owner);
		ProjectMember ownerMembership = saveMember(project, owner, ProjectMemberRole.OWNER);
		ProjectMember memberMembership = saveMember(project, member, ProjectMemberRole.MEMBER);

		ProjectMemberListResponse response = projectService.findMembers(
				project.getId(),
				owner.getUserId()
		);

		assertThat(response.projectId()).isEqualTo(project.getId());
		assertThat(response.ownerId()).isEqualTo(owner.getUserId());
		assertThat(response.title()).isEqualTo("SynQ");
		assertThat(response.currentMemberCount()).isEqualTo(2);
		assertThat(response.maxMemberCount()).isEqualTo(10);
		assertThat(response.members())
				.extracting(memberResponse -> memberResponse.memberId())
				.containsExactly(ownerMembership.getId(), memberMembership.getId());
		assertThat(response.members())
				.extracting(memberResponse -> memberResponse.joinedAt())
				.isSorted();
		assertThat(response.members().get(0).nickname()).isEqualTo("소유자");
		assertThat(response.members().get(0).role()).isEqualTo("OWNER");
		assertThat(response.members().get(0).isMe()).isTrue();
		assertThat(response.members().get(1).role()).isEqualTo("MEMBER");
		assertThat(response.members().get(1).isMe()).isFalse();
	}

	@Test
	void MEMBER도_멤버_목록을_조회하고_본인에게만_isMe를_반환한다() {
		User owner = saveUser("소유자", "member-list-member-owner@synq.com");
		User member = saveUser("멤버", "member-list-current-member@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(project, member, ProjectMemberRole.MEMBER);

		ProjectMemberListResponse response = projectService.findMembers(
				project.getId(),
				member.getUserId()
		);

		assertThat(response.members())
				.filteredOn(memberResponse -> memberResponse.isMe())
				.singleElement()
				.extracting(memberResponse -> memberResponse.userId())
				.isEqualTo(member.getUserId());
	}

	@Test
	void 프로젝트_외부_사용자는_멤버_목록을_조회할_수_없다() {
		User owner = saveUser("소유자", "member-list-outsider-owner@synq.com");
		User outsider = saveUser("외부 사용자", "member-list-outsider@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);

		assertThatThrownBy(() -> projectService.findMembers(project.getId(), outsider.getUserId()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.NOT_PROJECT_MEMBER));
	}

	@Test
	void 존재하지_않는_프로젝트는_멤버_목록을_조회할_수_없다() {
		User user = saveUser("사용자", "member-list-missing-project@synq.com");

		assertThatThrownBy(() -> projectService.findMembers(Long.MAX_VALUE, user.getUserId()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.PROJECT_NOT_FOUND));
	}

	@Test
	void 인증_정보가_없으면_멤버_목록을_조회할_수_없다() {
		assertThatThrownBy(() -> projectService.findMembers(1L, null))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(GeneralErrorCode.UNAUTHORIZED));
	}

	@Test
	void 존재하지_않는_사용자는_멤버_목록을_조회할_수_없다() {
		assertThatThrownBy(() -> projectService.findMembers(1L, Long.MAX_VALUE))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.USER_NOT_FOUND));
	}

	private Project saveProject(User owner) {
		return projectRepository.save(Project.of(owner.getUserId(), "SynQ", "프로젝트 설명"));
	}

	private ProjectMember saveMember(Project project, User user, ProjectMemberRole role) {
		return projectMemberRepository.save(
				ProjectMember.of(project.getId(), user.getUserId(), role));
	}

	private User saveUser(String name, String email) {
		return userRepository.save(User.ofLocal(name, email, "password-hash"));
	}
}
