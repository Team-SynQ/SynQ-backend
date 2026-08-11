package com.synq.backend.domain.project.service;

import com.synq.backend.domain.project.code.ProjectErrorCode;
import com.synq.backend.domain.project.dto.ProjectRolePerspectiveResponse;
import com.synq.backend.domain.project.dto.ProjectRolePerspectiveUpdateRequest;
import com.synq.backend.domain.project.dto.ProjectRolePerspectiveUpdateResponse;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.project.entity.ProjectMemberRole;
import com.synq.backend.domain.project.repository.ProjectMemberPerspectiveRepository;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.user.entity.Perspective;
import com.synq.backend.domain.user.entity.Role;
import com.synq.backend.domain.user.entity.RoleProfile;
import com.synq.backend.domain.user.entity.RoleProfilePerspective;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.RoleProfilePerspectiveRepository;
import com.synq.backend.domain.user.repository.RoleProfileRepository;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.global.apipayload.exception.GeneralException;
import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class ProjectRolePerspectiveServiceTest extends PostgresTestContainer {

	@Autowired
	private ProjectService projectService;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;

	@Autowired
	private ProjectMemberPerspectiveRepository projectMemberPerspectiveRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleProfileRepository roleProfileRepository;

	@Autowired
	private RoleProfilePerspectiveRepository roleProfilePerspectiveRepository;

	@Test
	void 프로젝트_설정이_없으면_사용자_기본_역할_관점을_반환한다() {
		User user = saveUser("role-default@synq.com");
		Project project = saveProjectWithMember(user);
		saveDefaultProfile(user, Role.DEV_TECH, "백엔드 개발자",
				List.of(Perspective.TECH_RISK, Perspective.ACTION_ITEM));

		ProjectRolePerspectiveResponse response = projectService.findRolePerspective(
				project.getId(), user.getUserId());

		assertThat(response.useDefault()).isTrue();
		assertThat(response.roleCategory()).isEqualTo(Role.DEV_TECH);
		assertThat(response.detailRole()).isEqualTo("백엔드 개발자");
		assertThat(response.perspectives()).containsExactly(
				Perspective.TECH_RISK, Perspective.ACTION_ITEM);
	}

	@Test
	void 기본_프로필이_없으면_null_역할과_빈_관점을_반환한다() {
		User user = saveUser("role-empty@synq.com");
		Project project = saveProjectWithMember(user);

		ProjectRolePerspectiveResponse response = projectService.findRolePerspective(
				project.getId(), user.getUserId());

		assertThat(response.useDefault()).isTrue();
		assertThat(response.roleCategory()).isNull();
		assertThat(response.detailRole()).isNull();
		assertThat(response.perspectives()).isEmpty();
	}

	@Test
	void 프로젝트별_역할_관점을_수정해도_사용자_기본_프로필은_변경되지_않는다() {
		User user = saveUser("role-custom@synq.com");
		Project project = saveProjectWithMember(user);
		RoleProfile defaultProfile = saveDefaultProfile(user, Role.DEV_TECH, "백엔드",
				List.of(Perspective.TECH_RISK));
		ProjectRolePerspectiveUpdateRequest request = new ProjectRolePerspectiveUpdateRequest(
				false,
				Role.DESIGN_CONTENT,
				"프로덕트 디자이너",
				List.of(Perspective.UX, Perspective.CUSTOMER_REACTION)
		);

		ProjectRolePerspectiveUpdateResponse response = projectService.updateRolePerspective(
				project.getId(), user.getUserId(), request);

		assertThat(response.projectId()).isEqualTo(project.getId());
		assertThat(response.useDefault()).isFalse();
		assertThat(response.roleCategory()).isEqualTo(Role.DESIGN_CONTENT);
		assertThat(response.detailRole()).isEqualTo("프로덕트 디자이너");
		assertThat(response.perspectives()).containsExactly(
				Perspective.UX, Perspective.CUSTOMER_REACTION);
		assertThat(response.updatedAt()).isNotNull();
		ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(
				project.getId(), user.getUserId()).orElseThrow();
		assertThat(projectMemberPerspectiveRepository.findAllByProjectMemberIdOrderByIdAsc(member.getId()))
				.extracting(projectMemberPerspective -> projectMemberPerspective.getPerspective())
				.containsExactly(Perspective.UX, Perspective.CUSTOMER_REACTION);
		assertThat(roleProfileRepository.findById(defaultProfile.getId()).orElseThrow().getRole())
				.isEqualTo(Role.DEV_TECH);
		assertThat(roleProfilePerspectiveRepository.findAllByRoleProfileId(defaultProfile.getId()))
				.extracting(RoleProfilePerspective::getPerspective)
				.containsExactly(Perspective.TECH_RISK);
	}

	@Test
	void 일반_MEMBER도_자신의_프로젝트별_역할_관점을_수정할_수_있다() {
		User owner = saveUser("role-member-owner@synq.com");
		User memberUser = saveUser("role-member@synq.com");
		Project project = saveProjectWithMember(owner);
		projectMemberRepository.save(ProjectMember.of(
				project.getId(), memberUser.getUserId(), ProjectMemberRole.MEMBER));

		ProjectRolePerspectiveUpdateResponse response = projectService.updateRolePerspective(
				project.getId(),
				memberUser.getUserId(),
				new ProjectRolePerspectiveUpdateRequest(
						false,
						Role.DATA_RESEARCH,
						"데이터 분석가",
						List.of(Perspective.COST_PERFORMANCE)
				)
		);

		assertThat(response.roleCategory()).isEqualTo(Role.DATA_RESEARCH);
		assertThat(response.perspectives()).containsExactly(Perspective.COST_PERFORMANCE);
	}

	@Test
	void 기본_설정_사용은_현재_기본_프로필_값을_프로젝트에_복사한다() {
		User user = saveUser("role-copy@synq.com");
		Project project = saveProjectWithMember(user);
		RoleProfile defaultProfile = saveDefaultProfile(user, Role.DEV_TECH, "백엔드",
				List.of(Perspective.TECH_RISK));

		projectService.updateRolePerspective(project.getId(), user.getUserId(),
				new ProjectRolePerspectiveUpdateRequest(true, null, null, null));

		defaultProfile.update(Role.MARKETING_BRANDING, "마케터");
		roleProfilePerspectiveRepository.deleteAllByRoleProfileId(defaultProfile.getId());
		roleProfilePerspectiveRepository.flush();
		roleProfilePerspectiveRepository.save(
				RoleProfilePerspective.of(defaultProfile.getId(), Perspective.CUSTOMER_REACTION));
		roleProfileRepository.flush();

		ProjectRolePerspectiveResponse response = projectService.findRolePerspective(
				project.getId(), user.getUserId());

		assertThat(response.useDefault()).isTrue();
		assertThat(response.roleCategory()).isEqualTo(Role.DEV_TECH);
		assertThat(response.detailRole()).isEqualTo("백엔드");
		assertThat(response.perspectives()).containsExactly(Perspective.TECH_RISK);
	}

	@Test
	void 기타_역할의_세부_역할_누락과_중복_관점은_400_예외이다() {
		User user = saveUser("role-invalid@synq.com");
		Project project = saveProjectWithMember(user);

		assertInvalid(project, user, new ProjectRolePerspectiveUpdateRequest(
				false, Role.ETC, " ", List.of()));
		assertInvalid(project, user, new ProjectRolePerspectiveUpdateRequest(
				false, Role.DEV_TECH, null,
				List.of(Perspective.TECH_RISK, Perspective.TECH_RISK)));
	}

	@Test
	void 프로젝트_외부_사용자는_조회하거나_수정할_수_없다() {
		User owner = saveUser("role-owner@synq.com");
		User outsider = saveUser("role-outsider@synq.com");
		Project project = saveProjectWithMember(owner);

		assertThatThrownBy(() -> projectService.findRolePerspective(project.getId(), outsider.getUserId()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(ProjectErrorCode.NOT_PROJECT_MEMBER));
		assertThatThrownBy(() -> projectService.updateRolePerspective(
				project.getId(),
				outsider.getUserId(),
				new ProjectRolePerspectiveUpdateRequest(false, Role.DEV_TECH, null, List.of())
		)).isInstanceOfSatisfying(GeneralException.class,
				exception -> assertThat(exception.getCode()).isEqualTo(ProjectErrorCode.NOT_PROJECT_MEMBER));
	}

	@Test
	void 존재하지_않거나_삭제된_프로젝트는_404_예외이다() {
		User user = saveUser("role-project-not-found@synq.com");
		Project deletedProject = saveProjectWithMember(user);
		deletedProject.softDelete();
		projectRepository.flush();

		assertProjectNotFound(() -> projectService.findRolePerspective(Long.MAX_VALUE, user.getUserId()));
		assertProjectNotFound(() -> projectService.findRolePerspective(deletedProject.getId(), user.getUserId()));
	}

	private void assertInvalid(
			Project project,
			User user,
			ProjectRolePerspectiveUpdateRequest request
	) {
		assertThatThrownBy(() -> projectService.updateRolePerspective(
				project.getId(), user.getUserId(), request))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.INVALID_PROJECT_ROLE_PERSPECTIVE));
	}

	private void assertProjectNotFound(Runnable action) {
		assertThatThrownBy(action::run)
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(ProjectErrorCode.PROJECT_NOT_FOUND));
	}

	private Project saveProjectWithMember(User user) {
		Project project = projectRepository.save(Project.of(user.getUserId(), "SynQ", null));
		projectMemberRepository.save(ProjectMember.of(
				project.getId(), user.getUserId(), ProjectMemberRole.OWNER));
		return project;
	}

	private RoleProfile saveDefaultProfile(
			User user,
			Role role,
			String detailRole,
			List<Perspective> perspectives
	) {
		RoleProfile profile = roleProfileRepository.save(
				RoleProfile.of(user.getUserId(), role, detailRole, true));
		roleProfilePerspectiveRepository.saveAll(perspectives.stream()
				.map(perspective -> RoleProfilePerspective.of(profile.getId(), perspective))
				.toList());
		return profile;
	}

	private User saveUser(String email) {
		return userRepository.save(User.ofLocal("테스트", email, "password-hash"));
	}
}
