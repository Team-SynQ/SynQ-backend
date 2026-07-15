package com.synq.backend.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * pgvector 가 설치된 PostgreSQL 을 도커로 띄우는 통합 테스트 베이스.
 * DB 가 필요한 테스트는 이 클래스를 상속한다.
 */
@SpringBootTest
public abstract class PostgresTestContainer {

	// static 이라 테스트 클래스마다 새로 뜨지 않는다.
	private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
			DockerImageName.parse("pgvector/pgvector:pg16")
					.asCompatibleSubstituteFor("postgres"));

	static {
		POSTGRES.start();
	}

	@DynamicPropertySource
	static void datasourceProperties(DynamicPropertyRegistry registry) {
		// 컨테이너는 매번 랜덤 포트에 뜨므로 application.yml 에 고정할 수 없다.
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		// 테스트는 실제 외부 AI API를 호출하지 않는다. 키 검증을 통과할 더미 값을 넣는다.
		registry.add("gemini.api-key", () -> "test-key");
		registry.add("openai.api-key", () -> "test-key");
	}
}
