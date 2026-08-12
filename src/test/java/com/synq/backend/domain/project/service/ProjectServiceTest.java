package com.synq.backend.domain.project.service;

import com.synq.backend.domain.project.code.ProjectErrorCode;
import com.synq.backend.domain.project.dto.ProjectCreateRequest;
import com.synq.backend.domain.project.dto.ProjectCreateResponse;
import com.synq.backend.domain.project.dto.ProjectRolePerspectiveResponse;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.project.entity.ProjectMemberPerspective;
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
class ProjectServiceTest extends PostgresTestContainer {

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
	void 기본_프로필이_있으면_프로젝트_생성_시점의_OWNER_역할_관점을_복사한다() {
		User owner = saveUser("owner-role-copy@synq.com");
		RoleProfile defaultProfile = roleProfileRepository.save(
				RoleProfile.of(owner.getUserId(), Role.DEV_TECH, "백엔드 개발자", true));
		roleProfilePerspectiveRepository.saveAll(List.of(
				RoleProfilePerspective.of(defaultProfile.getId(), Perspective.TECH_RISK),
				RoleProfilePerspective.of(defaultProfile.getId(), Perspective.ACTION_ITEM)
		));

		ProjectCreateResponse response = projectService.create(
				owner.getUserId(), new ProjectCreateRequest("SynQ", null));

		ProjectMember member = projectMemberRepository
				.findByProjectIdAndUserId(response.projectId(), owner.getUserId())
				.orElseThrow();
		assertThat(member.isUseDefault()).isTrue();
		assertThat(member.getRoleCategory()).isEqualTo(Role.DEV_TECH);
		assertThat(member.getDetailRole()).isEqualTo("백엔드 개발자");
		assertThat(projectMemberPerspectiveRepository.findAllByProjectMemberIdOrderByIdAsc(member.getId()))
				.extracting(ProjectMemberPerspective::getPerspective)
				.containsExactly(Perspective.TECH_RISK, Perspective.ACTION_ITEM);

		defaultProfile.update(Role.MARKETING_BRANDING, "마케터");
		roleProfilePerspectiveRepository.deleteAllByRoleProfileId(defaultProfile.getId());
		roleProfilePerspectiveRepository.save(
				RoleProfilePerspective.of(defaultProfile.getId(), Perspective.CUSTOMER_REACTION));
		roleProfileRepository.flush();
		roleProfilePerspectiveRepository.flush();

		ProjectRolePerspectiveResponse rolePerspective = projectService.findRolePerspective(
				response.projectId(), owner.getUserId());

		assertThat(rolePerspective.useDefault()).isTrue();
		assertThat(rolePerspective.roleCategory()).isEqualTo(Role.DEV_TECH);
		assertThat(rolePerspective.detailRole()).isEqualTo("백엔드 개발자");
		assertThat(rolePerspective.perspectives())
				.containsExactly(Perspective.TECH_RISK, Perspective.ACTION_ITEM);
	}

	@Test
	void 기본_프로필이_없어도_기존_정책대로_OWNER를_생성한다() {
		User owner = saveUser("owner-without-profile@synq.com");

		ProjectCreateResponse response = projectService.create(
				owner.getUserId(), new ProjectCreateRequest("SynQ", null));

		ProjectMember member = projectMemberRepository
				.findByProjectIdAndUserId(response.projectId(), owner.getUserId())
				.orElseThrow();
		assertThat(member.isUseDefault()).isTrue();
		assertThat(member.getRoleCategory()).isNull();
		assertThat(member.getDetailRole()).isNull();
		assertThat(projectMemberPerspectiveRepository.findAllByProjectMemberIdOrderByIdAsc(member.getId()))
				.isEmpty();
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
