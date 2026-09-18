package com.synq.backend.domain.user.controller;

import com.synq.backend.domain.auth.jwt.JwtProvider;
import com.synq.backend.domain.user.entity.Perspective;
import com.synq.backend.domain.user.entity.Role;
import com.synq.backend.domain.user.entity.RoleProfile;
import com.synq.backend.domain.user.entity.RoleProfilePerspective;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.RoleProfilePerspectiveRepository;
import com.synq.backend.domain.user.repository.RoleProfileRepository;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class RoleProfileControllerTest extends PostgresTestContainer {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleProfileRepository roleProfileRepository;

	@Autowired
	private RoleProfilePerspectiveRepository perspectiveRepository;

	@Autowired
	private JwtProvider jwtProvider;

	@Test
	void 프로필을_추가하면_201과_생성된_정보를_반환한다() throws Exception {
		User user = saveUser("create@synq.com");

		mockMvc.perform(post("/users/me/role-profiles")
						.header("Authorization", bearerToken(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"role":"DEV_TECH","detailRole":null,"perspectives":["SCHEDULE","TECH_RISK"]}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.result.isDefault").value(true))
				.andExpect(jsonPath("$.result.role").value("DEV_TECH"))
				.andExpect(jsonPath("$.result.perspectives[0]").value("SCHEDULE"));
	}

	@Test
	void role이_ETC인데_detailRole이_없으면_400을_반환한다() throws Exception {
		User user = saveUser("etc@synq.com");

		mockMvc.perform(post("/users/me/role-profiles")
						.header("Authorization", bearerToken(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"role":"ETC","perspectives":[]}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("USER400_1"));
	}

	@Test
	void 토큰_없이_요청하면_401을_반환한다() throws Exception {
		mockMvc.perform(get("/users/me/role-profiles"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 목록을_조회하면_등록순으로_반환한다() throws Exception {
		User user = saveUser("list@synq.com");
		saveProfile(user, Role.DEV_TECH, true);
		saveProfile(user, Role.DATA_RESEARCH, false);

		mockMvc.perform(get("/users/me/role-profiles")
						.header("Authorization", bearerToken(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.length()").value(2))
				.andExpect(jsonPath("$.result[0].role").value("DEV_TECH"))
				.andExpect(jsonPath("$.result[0].isDefault").value(true))
				.andExpect(jsonPath("$.result[1].role").value("DATA_RESEARCH"))
				.andExpect(jsonPath("$.result[1].isDefault").value(false));
	}

	@Test
	void 동일한_관점을_유지하며_프로필을_수정하면_200과_수정된_정보를_반환한다() throws Exception {
		User user = saveUser("update@synq.com");
		RoleProfile profile = roleProfileRepository.save(
				RoleProfile.of(user.getUserId(), Role.DEV_TECH, null, true));
		RoleProfilePerspective previous = perspectiveRepository.saveAndFlush(
				RoleProfilePerspective.of(profile.getId(), Perspective.SCOPE));

		mockMvc.perform(patch("/users/me/role-profiles/{profileId}", profile.getId())
						.header("Authorization", bearerToken(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"role":"STRATEGY_MANAGEMENT","detailRole":null,"perspectives":["SCOPE"]}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.id").value(profile.getId()))
				.andExpect(jsonPath("$.result.role").value("STRATEGY_MANAGEMENT"))
				.andExpect(jsonPath("$.result.perspectives[0]").value("SCOPE"));
		perspectiveRepository.flush();

		assertThat(roleProfileRepository.findById(profile.getId()).orElseThrow().getRole())
				.isEqualTo(Role.STRATEGY_MANAGEMENT);
		assertThat(perspectiveRepository.findAllByRoleProfileId(profile.getId()))
				.singleElement()
				.satisfies(saved -> {
					assertThat(saved.getId()).isNotEqualTo(previous.getId());
					assertThat(saved.getPerspective()).isEqualTo(Perspective.SCOPE);
				});
	}

	@Test
	void 다른_유저의_프로필을_수정하려하면_404를_반환한다() throws Exception {
		User owner = saveUser("update-owner@synq.com");
		User other = saveUser("update-intruder@synq.com");
		RoleProfile profile = saveProfile(owner, Role.DEV_TECH, true);

		mockMvc.perform(patch("/users/me/role-profiles/{profileId}", profile.getId())
						.header("Authorization", bearerToken(other))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"role":"STRATEGY_MANAGEMENT","perspectives":["SCOPE"]}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("USER404_1"));
	}

	@Test
	void 프로필_수정_요청값이_올바르지_않으면_400을_반환한다() throws Exception {
		User user = saveUser("update-invalid@synq.com");
		RoleProfile profile = saveProfile(user, Role.DEV_TECH, true);

		mockMvc.perform(patch("/users/me/role-profiles/{profileId}", profile.getId())
						.header("Authorization", bearerToken(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"role":"ETC","perspectives":[]}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("USER400_1"));
	}

	@Test
	void 기본_프로필을_삭제하려하면_400을_반환한다() throws Exception {
		User user = saveUser("delete-default@synq.com");
		RoleProfile profile = saveProfile(user, Role.DEV_TECH, true);

		mockMvc.perform(delete("/users/me/role-profiles/" + profile.getId())
						.header("Authorization", bearerToken(user)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("USER400_3"));
	}

	@Test
	void 다른_유저의_프로필을_삭제하려하면_404를_반환한다() throws Exception {
		User owner = saveUser("owner@synq.com");
		User other = saveUser("intruder@synq.com");
		saveProfile(owner, Role.DEV_TECH, true);
		RoleProfile ownerSecond = saveProfile(owner, Role.DATA_RESEARCH, false);

		mockMvc.perform(delete("/users/me/role-profiles/" + ownerSecond.getId())
						.header("Authorization", bearerToken(other)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("USER404_1"));
	}

	private RoleProfile saveProfile(User user, Role role, boolean isDefault) {
		RoleProfile profile = roleProfileRepository.save(RoleProfile.of(user.getUserId(), role, null, isDefault));
		perspectiveRepository.save(RoleProfilePerspective.of(profile.getId(), Perspective.SCHEDULE));
		return profile;
	}

	private String bearerToken(User user) {
		return "Bearer " + jwtProvider.createAccessToken(user.getUserId());
	}

	private User saveUser(String email) {
		return userRepository.save(User.ofLocal("테스트", email, "password-hash"));
	}
}
