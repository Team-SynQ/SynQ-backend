package com.synq.backend.domain.ai.assistant.application;

import static org.assertj.core.api.Assertions.assertThat;

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

/**
 * 매핑 검증(영구)과 행 선택 검증(임시)을 구분해 둔다.
 * 프로젝트별 역할·관점이 들어오면 선택 규칙만 바뀌고 매핑 규칙은 그대로 유효하다.
 */
@Transactional
class MemberProfileReaderTest extends PostgresTestContainer {

	@Autowired
	MemberProfileReader reader;
	@Autowired
	UserRepository userRepository;
	@Autowired
	RoleProfileRepository roleProfileRepository;
	@Autowired
	RoleProfilePerspectiveRepository perspectiveRepository;

	@Test
	void 역할과_세부역할과_관점을_그대로_담는다() {
		Long userId = saveUser();
		saveProfile(userId, Role.DEV_TECH, "백엔드", true, Perspective.TECH_RISK, Perspective.SCHEDULE);

		MemberProfile result = reader.find(1L, userId);

		assertThat(result.role()).isEqualTo("DEV_TECH");
		assertThat(result.detailRole()).isEqualTo("백엔드");
		assertThat(result.perspectives()).containsExactlyInAnyOrder("TECH_RISK", "SCHEDULE");
	}

	@Test
	void 세부역할이_null_이면_빈_문자열이_된다() {
		Long userId = saveUser();
		saveProfile(userId, Role.DEV_TECH, null, true);

		MemberProfile result = reader.find(1L, userId);

		assertThat(result.detailRole()).isEmpty();
	}

	@Test
	void 관점이_없으면_빈_리스트를_돌려준다() {
		Long userId = saveUser();
		saveProfile(userId, Role.ETC, "기타 역할", true);

		MemberProfile result = reader.find(1L, userId);

		assertThat(result.role()).isEqualTo("ETC");
		assertThat(result.perspectives()).isEmpty();
	}

	@Test
	void 역할_정보가_없으면_빈_프로필을_돌려준다() {
		Long userId = saveUser();

		MemberProfile result = reader.find(1L, userId);

		assertThat(result).isEqualTo(MemberProfile.empty());
	}

	/**
	 * 임시 동작 검증. 프로젝트 참여 시 기본 프로필을 복사해 프로젝트별 역할·관점을 저장하는
	 * 기능이 들어오면 이 테스트를 삭제하고 프로젝트별 값을 읽는 테스트로 대체한다.
	 */
	@Test
	void 프로젝트별_관점이_구현되기_전까지는_유저_기본_프로필을_읽는다() {
		Long userId = saveUser();
		saveProfile(userId, Role.PLANNING_OPERATION, "기본", true, Perspective.SCOPE);
		saveProfile(userId, Role.DEV_TECH, "기본_아님", false, Perspective.TECH_RISK);

		MemberProfile result = reader.find(1L, userId);

		assertThat(result.role()).isEqualTo("PLANNING_OPERATION");
		assertThat(result.perspectives()).containsExactly("SCOPE");
	}

	private Long saveUser() {
		return userRepository.save(
				User.ofLocal("테스트", UUID.randomUUID() + "@synq.com", "password-hash")).getUserId();
	}

	private void saveProfile(
			Long userId,
			Role role,
			String detailRole,
			boolean isDefault,
			Perspective... perspectives
	) {
		RoleProfile profile = roleProfileRepository.save(
				RoleProfile.of(userId, role, detailRole, isDefault));
		for (Perspective perspective : perspectives) {
			perspectiveRepository.save(RoleProfilePerspective.of(profile.getId(), perspective));
		}
	}
}
