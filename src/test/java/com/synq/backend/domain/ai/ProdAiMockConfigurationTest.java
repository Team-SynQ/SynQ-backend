package com.synq.backend.domain.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.synq.backend.BackendApplication;
import com.synq.backend.domain.ai.assistant.domain.AiChatClient;
import com.synq.backend.domain.ai.assistant.domain.HintAiClient;
import com.synq.backend.domain.ai.rag.port.ReferenceMaterialPort;
import com.synq.backend.domain.ai.summary.domain.MeetingSummaryStore;
import com.synq.backend.domain.ai.summary.domain.RagContextReader;
import com.synq.backend.domain.ai.summary.domain.SummaryAiClient;
import com.synq.backend.domain.ai.summary.domain.SummaryJobStore;
import com.synq.backend.domain.ai.summary.domain.TranscriptReader;
import com.synq.backend.domain.meeting.port.ProjectMembershipChecker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(classes = BackendApplication.class)
@ActiveProfiles("prod")
@Import(ProdAiMockConfigurationTest.ProductionTestConfig.class)
class ProdAiMockConfigurationTest {

	private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
			DockerImageName.parse("pgvector/pgvector:pg16")
					.asCompatibleSubstituteFor("postgres"));

	static {
		POSTGRES.start();
	}

	@Autowired
	private ApplicationContext applicationContext;

	@DynamicPropertySource
	static void productionProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("jwt.secret", () -> "prod-test-jwt-secret-at-least-thirty-two-characters");
		registry.add("gemini.api-key", () -> "test-key");
		registry.add("ai.chat.client", () -> "fake");
		registry.add("ai.summary.client", () -> "fake");
		registry.add("ai.live-context.client", () -> "fake");
		registry.add("ai.assistant.client", () -> "fake");
		registry.add("kakao.client-id", () -> "test");
		registry.add("kakao.redirect-uri", () -> "http://localhost/callback");
		registry.add("google.client-id", () -> "test");
		registry.add("google.redirect-uri", () -> "http://localhost/callback");
		registry.add("naver.client-id", () -> "test");
		registry.add("naver.redirect-uri", () -> "http://localhost/callback");
		registry.add("cors.allowed-origins", () -> "http://localhost:3000");
		registry.add("image.profile.cloudfront-domain", () -> "https://test-cdn.example.com");
	}

	@Test
	void prod_부트_설정에서_fake_mock_AI_포트가_각각_하나씩_등록된다() {
		assertThat(applicationContext.getBeansOfType(ReferenceMaterialPort.class)).hasSize(1);
		assertThat(applicationContext.getBeansOfType(AiChatClient.class)).hasSize(1);
		assertThat(applicationContext.getBeansOfType(HintAiClient.class)).hasSize(1);
		assertThat(applicationContext.getBeansOfType(SummaryAiClient.class)).hasSize(1);
		assertThat(applicationContext.getBeansOfType(SummaryJobStore.class)).hasSize(1);
		assertThat(applicationContext.getBeansOfType(MeetingSummaryStore.class)).hasSize(1);
		assertThat(applicationContext.getBeansOfType(TranscriptReader.class)).hasSize(1);
		assertThat(applicationContext.getBeansOfType(RagContextReader.class)).hasSize(1);
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class ProductionTestConfig {

		@Bean
		ProjectMembershipChecker projectMembershipChecker() {
			// 실제 project 도메인 어댑터가 추가되기 전까지 prod 컨텍스트 기동만 지원한다.
			return (projectId, userId) -> false;
		}
	}
}
