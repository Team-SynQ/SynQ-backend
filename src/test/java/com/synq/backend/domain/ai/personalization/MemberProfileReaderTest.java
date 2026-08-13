package com.synq.backend.domain.ai.personalization;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.synq.backend.support.PostgresTestContainer;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class MemberProfileReaderTest extends PostgresTestContainer {

	@Autowired
	MemberProfileReader reader;
	@Autowired
	UserRepository userRepository;
	@Autowired
	RoleProfileRepository roleProfileRepository;
	@Autowired
	RoleProfilePerspectiveRepository roleProfilePerspectiveRepository;
	@Autowired
	ProjectRepository projectRepository;
	@Autowired
	ProjectMemberRepository projectMemberRepository;
	@Autowired
	ProjectMemberPerspectiveRepository projectMemberPerspectiveRepository;

	@Test
	void 기본_설정을_사용하면_현재_유저_기본_프로필을_읽는다() {
		Long userId = saveUser();
		Long projectId = saveProjectMember(userId, true, Role.MARKETING_BRANDING, "프로젝트 복사본",
				Perspective.UX);
		saveDefaultProfile(userId, Role.DEV_TECH, "백엔드", Perspective.TECH_RISK, Perspective.SCHEDULE);

		MemberProfile result = reader.find(projectId, userId);

		assertThat(result.role()).isEqualTo("DEV_TECH");
		assertThat(result.detailRole()).isEqualTo("백엔드");
		assertThat(result.perspectives()).containsExactlyInAnyOrder("TECH_RISK", "SCHEDULE");
	}

	@Test
	void 프로젝트_설정을_사용하면_기본_프로필보다_프로젝트_값을_우선한다() {
		Long userId = saveUser();
		saveDefaultProfile(userId, Role.DEV_TECH, "기본 백엔드", Perspective.TECH_RISK);
		Long projectId = saveProjectMember(userId, false, Role.PLANNING_OPERATION, "프로젝트 PM",
				Perspective.SCOPE, Perspective.SCHEDULE);

		MemberProfile result = reader.find(projectId, userId);

		assertThat(result.role()).isEqualTo("PLANNING_OPERATION");
		assertThat(result.detailRole()).isEqualTo("프로젝트 PM");
		assertThat(result.perspectives()).containsExactly("SCOPE", "SCHEDULE");
	}

	@Test
	void 선택된_설정이_없으면_빈_프로필을_돌려준다() {
		Long userId = saveUser();
		Long projectId = saveProjectMember(userId, true, null, null);

		MemberProfile result = reader.find(projectId, userId);

		assertThat(result).isEqualTo(MemberProfile.empty());
	}

	@Test
	void 프로젝트_설정의_세부역할이_null이면_빈_문자열로_정규화한다() {
		Long userId = saveUser();
		Long projectId = saveProjectMember(userId, false, Role.ETC, null);

		MemberProfile result = reader.find(projectId, userId);

		assertThat(result.role()).isEqualTo("ETC");
		assertThat(result.detailRole()).isEmpty();
		assertThat(result.perspectives()).isEmpty();
	}

	private Long saveUser() {
		return userRepository.save(
				User.ofLocal("테스트", UUID.randomUUID() + "@synq.com", "password-hash")).getUserId();
	}

	private void saveDefaultProfile(Long userId, Role role, String detailRole, Perspective... perspectives) {
		RoleProfile profile = roleProfileRepository.save(RoleProfile.of(userId, role, detailRole, true));
		for (Perspective perspective : perspectives) {
			roleProfilePerspectiveRepository.save(RoleProfilePerspective.of(profile.getId(), perspective));
		}
	}

	private Long saveProjectMember(
			Long userId,
			boolean useDefault,
			Role role,
			String detailRole,
			Perspective... perspectives
	) {
		Project project = projectRepository.save(Project.of(userId, "개인화 테스트", null));
		ProjectMember member = ProjectMember.of(project.getId(), userId, ProjectMemberRole.OWNER);
		member.updateRolePerspective(useDefault, role, detailRole);
		projectMemberRepository.save(member);
		for (Perspective perspective : perspectives) {
			projectMemberPerspectiveRepository.save(ProjectMemberPerspective.of(member.getId(), perspective));
		}
		return project.getId();
	}
}
