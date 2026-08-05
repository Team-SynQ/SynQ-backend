package com.synq.backend.support;

import com.synq.backend.domain.ai.client.EmbeddingClient;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.reference.repository.ReferenceMaterialRepository;
import com.synq.backend.domain.user.repository.UserRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 통합 테스트가 실제 외부 API 를 호출하지 않도록 막는 대역 모음이다.
 * ReferenceMaterialPort 는 ReferenceMaterialAdapter 실구현이 생겨 대역을 두지 않는다.
 */
@TestConfiguration
public class TestPortConfig {

	/**
	 * 통합 테스트가 실제 Gemini 를 호출하지 않도록 막는다.
	 * PostgresTestContainer 는 더미 API 키만 넣을 뿐이라 이게 없으면 HTTP 요청이 실제로 나간다.
	 */
	@Bean
	@Primary
	public EmbeddingClient embeddingClient() {
		return new StubEmbeddingClient();
	}

	@Bean
	public ReferenceMaterialTestFixture referenceMaterialTestFixture(
			UserRepository userRepository,
			ProjectRepository projectRepository,
			ReferenceMaterialRepository referenceMaterialRepository
	) {
		return new ReferenceMaterialTestFixture(
				userRepository,
				projectRepository,
				referenceMaterialRepository
		);
	}
}
