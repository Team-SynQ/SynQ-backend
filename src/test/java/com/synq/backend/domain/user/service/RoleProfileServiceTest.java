package com.synq.backend.domain.user.service;

import com.synq.backend.domain.user.code.UserErrorCode;
import com.synq.backend.domain.user.dto.RoleProfileRequest;
import com.synq.backend.domain.user.dto.RoleProfileResponse;
import com.synq.backend.domain.user.entity.Perspective;
import com.synq.backend.domain.user.entity.Role;
import com.synq.backend.domain.user.entity.RoleProfilePerspective;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.RoleProfilePerspectiveRepository;
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

	@Autowired
	private RoleProfilePerspectiveRepository perspectiveRepository;

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
	void 동일한_관점을_유지하면서_역할을_수정할_수_있다() {
		User user = saveUser("update-same-perspective@synq.com");
		RoleProfileResponse profile = createProfile(user, List.of(Perspective.SCOPE));

		RoleProfileResponse response = roleProfileService.update(user.getUserId(), profile.id(),
				new RoleProfileRequest(Role.STRATEGY_MANAGEMENT, null, List.of(Perspective.SCOPE)));
		perspectiveRepository.flush();

		assertThat(response.role()).isEqualTo(Role.STRATEGY_MANAGEMENT);
		assertThat(response.perspectives()).containsExactly(Perspective.SCOPE);
		assertStoredPerspectives(profile.id(), Perspective.SCOPE);
	}

	@Test
	void 일부_관점을_유지하면서_나머지_관점을_교체할_수_있다() {
		User user = saveUser("update-partial-perspectives@synq.com");
		RoleProfileResponse profile = createProfile(user, List.of(Perspective.SCOPE, Perspective.UX));

		RoleProfileResponse response = roleProfileService.update(user.getUserId(), profile.id(),
				new RoleProfileRequest(Role.STRATEGY_MANAGEMENT, null,
						List.of(Perspective.SCOPE, Perspective.DECISION)));
		perspectiveRepository.flush();

		assertThat(response.perspectives()).containsExactly(Perspective.SCOPE, Perspective.DECISION);
		assertStoredPerspectives(profile.id(), Perspective.SCOPE, Perspective.DECISION);
	}

	@Test
	void 기존_관점을_전부_다른_관점으로_교체할_수_있다() {
		User user = saveUser("update-all-perspectives@synq.com");
		RoleProfileResponse profile = createProfile(user, List.of(Perspective.SCOPE));

		RoleProfileResponse response = roleProfileService.update(user.getUserId(), profile.id(),
				new RoleProfileRequest(Role.STRATEGY_MANAGEMENT, null, List.of(Perspective.DECISION)));
		perspectiveRepository.flush();

		assertThat(response.perspectives()).containsExactly(Perspective.DECISION);
		assertStoredPerspectives(profile.id(), Perspective.DECISION);
	}

	@Test
	void 기존_관점을_빈_배열로_교체할_수_있다() {
		User user = saveUser("update-empty-perspectives@synq.com");
		RoleProfileResponse profile = createProfile(user, List.of(Perspective.SCOPE));

		RoleProfileResponse response = roleProfileService.update(user.getUserId(), profile.id(),
				new RoleProfileRequest(Role.STRATEGY_MANAGEMENT, null, List.of()));
		perspectiveRepository.flush();

		assertThat(response.perspectives()).isEmpty();
		assertStoredPerspectives(profile.id());
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

	private RoleProfileResponse createProfile(User user, List<Perspective> perspectives) {
		return roleProfileService.create(user.getUserId(),
				new RoleProfileRequest(Role.DEV_TECH, null, perspectives));
	}

	private void assertStoredPerspectives(Long profileId, Perspective... perspectives) {
		assertThat(perspectiveRepository.findAllByRoleProfileId(profileId))
				.extracting(RoleProfilePerspective::getPerspective)
				.containsExactlyInAnyOrder(perspectives);
	}

	private User saveUser(String email) {
		return userRepository.save(User.ofLocal("테스트", email, "password-hash"));
	}
}
