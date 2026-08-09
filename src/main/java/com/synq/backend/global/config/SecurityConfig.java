package com.synq.backend.global.config;

import com.synq.backend.domain.auth.jwt.JwtAuthenticationFilter;
import com.synq.backend.domain.auth.jwt.JwtProvider;
import com.synq.backend.global.apipayload.handler.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
									"/projects/*/members",
									"/projects/*/references").authenticated()
							.requestMatchers(HttpMethod.POST,
									"/projects",
									"/projects/join",
									"/projects/*/invitation").authenticated()
							.requestMatchers(HttpMethod.POST,
									"/projects/*/references/links",
									"/projects/*/references/files").authenticated()
							.requestMatchers(HttpMethod.PATCH, "/projects/*").authenticated()
							.requestMatchers(HttpMethod.DELETE,
									"/projects/*",
									"/projects/*/members/*",
									"/projects/*/references/*").authenticated()
							.requestMatchers("/users/me", "/users/me/**").authenticated()
							.requestMatchers("/projects/*/meetings", "/meetings/*/end", "/meetings/*/join",
									"/meetings/*/title").authenticated()
							.requestMatchers("/meetings/*/ai-summary/**", "/meetings/*/summary/**").authenticated()
							.requestMatchers("/meetings/*/live-context", "/meetings/*/segments/*/hints").authenticated()
							.requestMatchers("/meetings/*/transcript-segments/**").authenticated()
							.requestMatchers("/meetings/*/transcript-reindex",
									"/reference-materials/*/reindex").authenticated()
							// 각 도메인 컨트롤러 테스트에 인증 절차 추가 이후 anyRequest().authenticated()로 전환 필요
							.anyRequest().permitAll())
				.exceptionHandling(exception -> exception.authenticationEntryPoint(entryPoint))
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	public JwtAuthenticationFilter jwtAuthenticationFilter(JwtProvider jwtProvider) {
		return new JwtAuthenticationFilter(jwtProvider);
	}
}
