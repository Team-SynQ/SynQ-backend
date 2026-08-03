package com.synq.backend.domain.ai.assistant.application;

import com.synq.backend.domain.user.entity.RoleProfile;
import com.synq.backend.domain.user.repository.RoleProfilePerspectiveRepository;
import com.synq.backend.domain.user.repository.RoleProfileRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 3-hint 와 AI Chat 이 공유하는 역할·관점 조회기.
 *
 * <p>최종 형태는 프로젝트별 역할·관점을 읽는 것이다. 사용자가 프로젝트에 참여할 때
 * 유저 기본 프로필 값이 프리필되고, 확인·수정해서 저장하면 그 값이 기본 프로필과 무관하게 독립적으로 산다.
 * 그 스키마가 아직 없어 지금은 유저 기본 프로필로 대신한다 — 폴백이 아니라 임시 대체다.
 *
 * <p>그럼에도 projectId 를 파라미터로 받는 이유는, 프로젝트별 값이 추가될 때
 * 이 클래스 한 곳만 고치면 두 기능이 함께 따라오게 하기 위해서다.
 */
@Component
public class MemberProfileReader {

	private final RoleProfileRepository roleProfileRepository;
	private final RoleProfilePerspectiveRepository perspectiveRepository;

	public MemberProfileReader(
			RoleProfileRepository roleProfileRepository,
			RoleProfilePerspectiveRepository perspectiveRepository
	) {
		this.roleProfileRepository = roleProfileRepository;
		this.perspectiveRepository = perspectiveRepository;
	}

	@Transactional(readOnly = true)
	public MemberProfile find(Long projectId, Long userId) {
		return roleProfileRepository.findByUserIdAndIsDefaultTrue(userId)
				.map(this::toProfile)
				.orElseGet(MemberProfile::empty);
	}

	private MemberProfile toProfile(RoleProfile profile) {
		List<String> perspectives = perspectiveRepository.findAllByRoleProfileId(profile.getId()).stream()
				.map(value -> value.getPerspective().name())
				.toList();
		return new MemberProfile(profile.getRole().name(), profile.getDetailRole(), perspectives);
	}
}
