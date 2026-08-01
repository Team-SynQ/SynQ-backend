package com.synq.backend.domain.user.controller;

import com.synq.backend.domain.auth.jwt.JwtProvider;
import com.synq.backend.domain.user.entity.Provider;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class UserControllerTest extends PostgresTestContainer {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JwtProvider jwtProvider;

	@Test
	void 내_정보를_조회하면_유저아이디_이름_이메일_로그인방식을_반환한다() throws Exception {
		User user = userRepository.save(User.ofLocal("테스트", "me@synq.com", "password-hash"));

		mockMvc.perform(get("/users/me")
						.header("Authorization", bearerToken(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.userId").value(user.getUserId()))
				.andExpect(jsonPath("$.result.name").value("테스트"))
				.andExpect(jsonPath("$.result.email").value("me@synq.com"))
				.andExpect(jsonPath("$.result.provider").value("LOCAL"));
	}

	@Test
	void 이메일이_없는_소셜유저는_이메일이_null로_반환된다() throws Exception {
		User user = userRepository.save(User.ofSocial("카카오유저", null, Provider.KAKAO, "kakao-id-1"));

		mockMvc.perform(get("/users/me")
						.header("Authorization", bearerToken(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.email").doesNotExist());
	}

	@Test
	void 토큰_없이_내_정보를_조회하면_401을_반환한다() throws Exception {
		mockMvc.perform(get("/users/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 이름을_변경하면_변경된_이름을_반환한다() throws Exception {
		User user = userRepository.save(User.ofLocal("변경전", "rename@synq.com", "password-hash"));

		mockMvc.perform(patch("/users/me/name")
						.header("Authorization", bearerToken(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"변경후"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.name").value("변경후"));
	}

	@Test
	void 이름이_비어있으면_400을_반환한다() throws Exception {
		User user = userRepository.save(User.ofLocal("테스트", "blank@synq.com", "password-hash"));

		mockMvc.perform(patch("/users/me/name")
						.header("Authorization", bearerToken(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":""}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void 이름이_20자를_초과하면_400을_반환한다() throws Exception {
		User user = userRepository.save(User.ofLocal("테스트", "toolong@synq.com", "password-hash"));

		mockMvc.perform(patch("/users/me/name")
						.header("Authorization", bearerToken(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"012345678901234567890"}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void 토큰_없이_이름을_변경하면_401을_반환한다() throws Exception {
		mockMvc.perform(patch("/users/me/name")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"이름"}
								"""))
				.andExpect(status().isUnauthorized());
	}

	private String bearerToken(User user) {
		return "Bearer " + jwtProvider.createAccessToken(user.getUserId());
	}
}
