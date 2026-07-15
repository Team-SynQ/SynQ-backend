package com.synq.backend.global.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI backendOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("SynQ API")
						.description("SynQ 백엔드 API 문서")
						.version("v0.0.1"));
	}
}
