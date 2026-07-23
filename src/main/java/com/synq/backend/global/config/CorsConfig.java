package com.synq.backend.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

	private final String[] allowedOrigins;

	public CorsConfig(CorsProperties properties) {
		String[] trimmed = properties.allowedOrigins().stream()
				.map(String::trim)
				.toArray(String[]::new);
		for (String origin : trimmed) {
			if ("*".equals(origin)) {
				throw new IllegalStateException("cors.allowed-origins 에는 와일드카드(*)를 쓸 수 없습니다. 허용할 origin을 각각 나열하세요.");
			}
		}
		this.allowedOrigins = trimmed;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
				.allowedOrigins(allowedOrigins)
				.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
				.allowedHeaders("*");
	}
}
