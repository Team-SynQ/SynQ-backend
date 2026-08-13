package com.synq.backend.global.config;

import com.synq.backend.domain.auth.jwt.JwtAuthenticationFilter;
import com.synq.backend.domain.auth.jwt.JwtProvider;
import com.synq.backend.global.apipayload.handler.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private static final String[] PUBLIC_ENDPOINTS = {
			"/auth/**",
			"/swagger-ui/**",
			"/swagger-ui.html",
			"/v3/api-docs/**",
			"/actuator/health"
	};

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter,
													RestAuthenticationEntryPoint entryPoint) throws Exception {
		http
				// CorsConfig(WebMvcConfigurer)의 설정은 DispatcherServlet 까지 가야 적용된다. 이 선언이 없으면
				// Authorization 헤더가 없는 preflight(OPTIONS)가 인가 필터에서 401 로 끊겨 프론트에 CORS 에러로 보인다.
				.cors(Customizer.withDefaults())
				.csrf(csrf -> csrf.disable())
				.formLogin(formLogin -> formLogin.disable())
				.httpBasic(httpBasic -> httpBasic.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(PUBLIC_ENDPOINTS).permitAll()
						// WS 는 브라우저가 Authorization 헤더를 못 붙여 SttHandshakeInterceptor 가 쿼리파라미터
						// 토큰으로 직접 인증한다. anyRequest().authenticated() 로 전환해도 막히지 않도록 명시한다.
							.requestMatchers("/ws/**").permitAll()
							.requestMatchers(HttpMethod.GET, "/projects/invitations/*").permitAll()
							.requestMatchers(HttpMethod.GET,
									"/projects",
									"/projects/*",
									"/projects/join-requests/me",
									"/projects/*/members",
									"/projects/*/join-requests",
									"/projects/*/role-perspective",
									"/projects/*/references").authenticated()
							.requestMatchers(HttpMethod.POST,
									"/projects",
									"/projects/join",
									"/projects/*/invitation",
									"/projects/*/join-requests").authenticated()
							.requestMatchers(HttpMethod.POST,
									"/projects/*/references/links",
									"/projects/*/references/files").authenticated()
							.requestMatchers(HttpMethod.PATCH,
									"/projects/*",
									"/projects/*/references/*",
									"/projects/*/join-requests/*/approve",
									"/projects/*/join-requests/*/reject").authenticated()
							.requestMatchers(HttpMethod.PUT,
									"/projects/*/role-perspective").authenticated()
							.requestMatchers(HttpMethod.DELETE,
									"/projects/*",
									"/projects/*/members/*",
									"/projects/*/references/*").authenticated()
							.requestMatchers("/users/me", "/users/me/**").authenticated()
							.requestMatchers("/projects/*/meetings", "/meetings/*", "/meetings/*/end", "/meetings/*/join",
									"/meetings/*/title", "/meetings/*/leave", "/meetings/*/participants").authenticated()
							.requestMatchers("/meetings/*/ai-summary/**", "/meetings/*/summary/**").authenticated()
							.requestMatchers("/meetings/*/live-context", "/meetings/*/segments/*/hints").authenticated()
							.requestMatchers("/meetings/*/transcript-segments/**").authenticated()
							.requestMatchers("/meetings/*/transcript-reindex",
									"/reference-materials/*/reindex").authenticated()
							// 공개 범위에 명시되지 않은 새 API가 인증 없이 노출되지 않도록 기본값을 인증 필요로 둔다.
							.anyRequest().authenticated())
				.exceptionHandling(exception -> exception.authenticationEntryPoint(entryPoint))
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	public JwtAuthenticationFilter jwtAuthenticationFilter(JwtProvider jwtProvider) {
		return new JwtAuthenticationFilter(jwtProvider);
	}
}
