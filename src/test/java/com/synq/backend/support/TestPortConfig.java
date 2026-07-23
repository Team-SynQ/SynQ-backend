package com.synq.backend.support;

import com.synq.backend.domain.ai.client.EmbeddingClient;
import com.synq.backend.domain.ai.rag.port.ReferenceMaterialPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * ReferenceMaterialPort 구현체를 제공하기 전까지, 통합 테스트에서 컨텍스트가 뜨도록
 * 대역을 빈으로 등록한다. 해당 구현이 들어오면 이 설정은 삭제한다.
 */
@TestConfiguration
public class TestPortConfig {

	@Bean
	@Primary
	public ReferenceMaterialPort referenceMaterialPort() {
		return new StubReferenceMaterialPort();
	}

	/**
	 * 통합 테스트가 실제 Gemini 를 호출하지 않도록 막는다.
	 * PostgresTestContainer 는 더미 API 키만 넣을 뿐이라 이게 없으면 HTTP 요청이 실제로 나간다.
	 */
	@Bean
	@Primary
	public EmbeddingClient embeddingClient() {
		return new StubEmbeddingClient();
	}
}
