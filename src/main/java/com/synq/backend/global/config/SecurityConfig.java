package com.synq.backend.global.config;

import com.synq.backend.domain.auth.jwt.JwtAuthenticationFilter;
import com.synq.backend.domain.auth.jwt.JwtProvider;
import com.synq.backend.global.apipayload.handler.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
			"/v3/api-docs/**"
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
