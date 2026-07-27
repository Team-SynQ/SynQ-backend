package com.synq.backend.domain.user.service;

import com.synq.backend.domain.user.code.UserErrorCode;
import com.synq.backend.domain.user.dto.RoleProfileRequest;
import com.synq.backend.domain.user.dto.RoleProfileResponse;
import com.synq.backend.domain.user.entity.Perspective;
import com.synq.backend.domain.user.entity.Role;
import com.synq.backend.domain.user.entity.User;
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
class RoleProfileServiceTest extends PostgresTestContainer {

	@Autowired
	private RoleProfileService roleProfileService;

	@Autowired
	private UserRepository userRepository;

	@Test
	void 첫_프로필은_자동으로_기본이_된다() {
		User user = saveUser("first-profile@synq.com");

		RoleProfileResponse response = roleProfileService.create(user.getUserId(),
				new RoleProfileRequest(Role.DEV_TECH, null, List.of(Perspective.SCHEDULE)));

		assertThat(response.isDefault()).isTrue();
		assertThat(response.role()).isEqualTo(Role.DEV_TECH);
		assertThat(response.perspectives()).containsExactly(Perspective.SCHEDULE);
	}

	@Test
	void 두번째_프로필은_기본이_아니다() {
		User user = saveUser("second-profile@synq.com");
		roleProfileService.create(user.getUserId(), new RoleProfileRequest(Role.DEV_TECH, null, List.of()));

		RoleProfileResponse second = roleProfileService.create(user.getUserId(),
				new RoleProfileRequest(Role.DATA_RESEARCH, null, List.of(Perspective.SCOPE)));

		assertThat(second.isDefault()).isFalse();
	}

	@Test
	void role이_ETC인데_detailRole이_없으면_예외를_던진다() {
		User user = saveUser("etc-required@synq.com");

		assertThatThrownBy(() -> roleProfileService.create(user.getUserId(),
				new RoleProfileRequest(Role.ETC, null, List.of())))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(UserErrorCode.DETAIL_ROLE_REQUIRED));
	}

	@Test
	void 관점이_3개를_초과하면_예외를_던진다() {
		User user = saveUser("too-many-perspectives@synq.com");

		assertThatThrownBy(() -> roleProfileService.create(user.getUserId(),
				new RoleProfileRequest(Role.DEV_TECH, null,
						List.of(Perspective.SCHEDULE, Perspective.SCOPE, Perspective.DECISION, Perspective.UX))))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(UserErrorCode.TOO_MANY_PERSPECTIVES));
	}

	@Test
	void 다른_유저의_프로필을_수정하려하면_예외를_던진다() {
		User owner = saveUser("owner@synq.com");
		User other = saveUser("other@synq.com");
		RoleProfileResponse profile = roleProfileService.create(owner.getUserId(),
				new RoleProfileRequest(Role.DEV_TECH, null, List.of()));

		assertThatThrownBy(() -> roleProfileService.update(other.getUserId(), profile.id(),
				new RoleProfileRequest(Role.DATA_RESEARCH, null, List.of())))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(UserErrorCode.ROLE_PROFILE_NOT_FOUND));
	}

	@Test
	void 기본_프로필을_삭제하려하면_예외를_던진다() {
		User user = saveUser("delete-default@synq.com");
		RoleProfileResponse profile = roleProfileService.create(user.getUserId(),
				new RoleProfileRequest(Role.DEV_TECH, null, List.of()));

		assertThatThrownBy(() -> roleProfileService.delete(user.getUserId(), profile.id()))
				.isInstanceOfSatisfying(GeneralException.class, exception -> assertThat(exception.getCode())
						.isEqualTo(UserErrorCode.CANNOT_DELETE_DEFAULT_ROLE_PROFILE));
	}

	@Test
	void 기본이_아닌_프로필은_삭제된다() {
		User user = saveUser("delete-non-default@synq.com");
		roleProfileService.create(user.getUserId(), new RoleProfileRequest(Role.DEV_TECH, null, List.of()));
		RoleProfileResponse second = roleProfileService.create(user.getUserId(),
				new RoleProfileRequest(Role.DATA_RESEARCH, null, List.of()));

		roleProfileService.delete(user.getUserId(), second.id());

		assertThat(roleProfileService.getMyRoleProfiles(user.getUserId())).hasSize(1);
	}

	@Test
	void 기본으로_설정하면_기존_기본은_해제된다() {
		User user = saveUser("set-default@synq.com");
		RoleProfileResponse first = roleProfileService.create(user.getUserId(),
				new RoleProfileRequest(Role.DEV_TECH, null, List.of()));
		RoleProfileResponse second = roleProfileService.create(user.getUserId(),
				new RoleProfileRequest(Role.DATA_RESEARCH, null, List.of()));

		roleProfileService.setDefault(user.getUserId(), second.id());

		List<RoleProfileResponse> profiles = roleProfileService.getMyRoleProfiles(user.getUserId());
		assertThat(profiles).filteredOn(profile -> profile.id().equals(first.id()))
				.singleElement().satisfies(profile -> assertThat(profile.isDefault()).isFalse());
		assertThat(profiles).filteredOn(profile -> profile.id().equals(second.id()))
				.singleElement().satisfies(profile -> assertThat(profile.isDefault()).isTrue());
	}

	@Test
	void 존재하지_않는_프로필을_기본으로_설정하려하면_예외를_던진다() {
		User user = saveUser("not-found@synq.com");

		assertThatThrownBy(() -> roleProfileService.setDefault(user.getUserId(), 999_999L))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(UserErrorCode.ROLE_PROFILE_NOT_FOUND));
	}

	private User saveUser(String email) {
		return userRepository.save(User.ofLocal("테스트", email, "password-hash"));
	}
}
