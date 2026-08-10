package com.synq.backend.global.config;

import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 브라우저 preflight(OPTIONS)에는 Authorization 헤더가 실리지 않는다. 시큐리티 체인이 CORS 를 처리하지
 * 않으면 인증이 필요한 엔드포인트의 preflight 가 401 로 끊겨 프론트에는 CORS 에러로 보인다.
 */
@AutoConfigureMockMvc
class CorsPreflightTest extends PostgresTestContainer {

	private static final String ORIGIN = "http://localhost:3000";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void permitAll_엔드포인트의_preflight를_허용한다() throws Exception {
		mockMvc.perform(options("/auth/login")
						.header("Origin", ORIGIN)
						.header("Access-Control-Request-Method", "POST")
						.header("Access-Control-Request-Headers", "content-type"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", ORIGIN));
	}

	@Test
	void 인증이_필요한_유저_엔드포인트의_preflight를_토큰_없이도_허용한다() throws Exception {
		mockMvc.perform(options("/users/me")
						.header("Origin", ORIGIN)
						.header("Access-Control-Request-Method", "GET")
						.header("Access-Control-Request-Headers", "authorization"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", ORIGIN));
	}

	@Test
	void 인증이_필요한_프로젝트_엔드포인트의_preflight를_토큰_없이도_허용한다() throws Exception {
		mockMvc.perform(options("/projects")
						.header("Origin", ORIGIN)
						.header("Access-Control-Request-Method", "POST")
						.header("Access-Control-Request-Headers", "authorization,content-type"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", ORIGIN));
	}

	@Test
	void 인증이_필요한_회의_엔드포인트의_preflight를_토큰_없이도_허용한다() throws Exception {
		mockMvc.perform(options("/meetings/1/live-context")
						.header("Origin", ORIGIN)
						.header("Access-Control-Request-Method", "GET")
						.header("Access-Control-Request-Headers", "authorization"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", ORIGIN));
	}

	@Test
	void 허용되지_않은_origin_의_preflight는_거부한다() throws Exception {
		mockMvc.perform(options("/users/me")
						.header("Origin", "http://evil.example.com")
						.header("Access-Control-Request-Method", "GET")
						.header("Access-Control-Request-Headers", "authorization"))
				.andExpect(status().isForbidden());
	}

	@Test
	void preflight_허용이_실제_요청의_인증까지_풀어주지는_않는다() throws Exception {
		mockMvc.perform(get("/users/me")
						.header("Origin", ORIGIN))
				.andExpect(status().isUnauthorized());
	}
}
