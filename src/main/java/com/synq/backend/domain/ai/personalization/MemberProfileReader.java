package com.synq.backend.domain.ai.personalization;

import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.project.repository.ProjectMemberPerspectiveRepository;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.user.entity.RoleProfile;
import com.synq.backend.domain.user.repository.RoleProfilePerspectiveRepository;
import com.synq.backend.domain.user.repository.RoleProfileRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 기능이 공통으로 사용하는 프로젝트별 역할·관점 조회기다.
 * 프로젝트 멤버가 기본 설정을 사용하면 현재 기본 프로필을, 아니면 프로젝트 설정을 반환한다.
 */
@Component
public class MemberProfileReader {

	private final RoleProfileRepository roleProfileRepository;
	private final RoleProfilePerspectiveRepository roleProfilePerspectiveRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final ProjectMemberPerspectiveRepository projectMemberPerspectiveRepository;

	public MemberProfileReader(
			RoleProfileRepository roleProfileRepository,
			RoleProfilePerspectiveRepository roleProfilePerspectiveRepository,
			ProjectMemberRepository projectMemberRepository,
			ProjectMemberPerspectiveRepository projectMemberPerspectiveRepository
	) {
		this.roleProfileRepository = roleProfileRepository;
		this.roleProfilePerspectiveRepository = roleProfilePerspectiveRepository;
		this.projectMemberRepository = projectMemberRepository;
		this.projectMemberPerspectiveRepository = projectMemberPerspectiveRepository;
	}

	@Transactional(readOnly = true)
	public MemberProfile find(Long projectId, Long userId) {
		return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
				.filter(member -> !member.isUseDefault())
				.map(this::toProjectProfile)
				.orElseGet(() -> findDefaultProfile(userId));
	}

	private MemberProfile findDefaultProfile(Long userId) {
		return roleProfileRepository.findByUserIdAndIsDefaultTrue(userId)
				.map(this::toDefaultProfile)
				.orElseGet(MemberProfile::empty);
	}

	private MemberProfile toDefaultProfile(RoleProfile profile) {
		List<String> perspectives = roleProfilePerspectiveRepository.findAllByRoleProfileId(profile.getId()).stream()
				.map(value -> value.getPerspective().name())
				.toList();
		return new MemberProfile(profile.getRole().name(), profile.getDetailRole(), perspectives);
	}

	private MemberProfile toProjectProfile(ProjectMember member) {
		List<String> perspectives = projectMemberPerspectiveRepository
				.findAllByProjectMemberIdOrderByIdAsc(member.getId()).stream()
				.map(value -> value.getPerspective().name())
				.toList();
		String role = member.getRoleCategory() == null ? "" : member.getRoleCategory().name();
		return new MemberProfile(role, member.getDetailRole(), perspectives);
	}
}
