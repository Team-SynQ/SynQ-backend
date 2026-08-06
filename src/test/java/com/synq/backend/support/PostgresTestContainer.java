package com.synq.backend.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * pgvector 가 설치된 PostgreSQL 을 도커로 띄우는 통합 테스트 베이스.
 * DB 가 필요한 테스트는 이 클래스를 상속한다.
 */
@SpringBootTest
@Import(TestPortConfig.class)
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
		// @MockitoBean 조합이 다르면 스프링이 컨텍스트를 따로 캐시하고, 컨텍스트마다 커넥션 풀이 생긴다.
		// 기본 10개로 두면 컨텍스트가 늘어날수록 컨테이너의 max_connections(100)를 넘겨
		// "too many clients already" 로 뒤늦게 뜨는 컨텍스트부터 기동이 깨진다.
		registry.add("spring.datasource.hikari.maximum-pool-size", () -> "4");
		registry.add("spring.datasource.hikari.minimum-idle", () -> "0");
		// 테스트는 실제 외부 AI API를 호출하지 않는다. 키 검증을 통과할 더미 값을 넣는다.
		registry.add("gemini.api-key", () -> "test-key");
		registry.add("openai.api-key", () -> "test-key");
		// 개발 셸의 AI 환경변수가 테스트 빈 구성을 바꾸지 않도록 테스트용 구현체를 고정한다.
		registry.add("ai.chat.client", () -> "fake");
		registry.add("ai.summary.client", () -> "fake");
		registry.add("ai.assistant.client", () -> "fake");
		// 통합 테스트는 MockWebServer(루프백)를 쓴다. SafeDns 기본 정책이 이를 막는다.
		registry.add("reference.link.allow-private-network", () -> "true");
	}
}
