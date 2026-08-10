package com.synq.backend.domain.ai.client.openai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synq.backend.domain.ai.summary.application.SummaryProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;

class OpenAiSummaryClientConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(OpenAiSummaryClientConfiguration.class)
			.withPropertyValues(
					"openai.api-key=test-key",
					"openai.base-url=https://api.openai.com/v1",
					"openai.model=default-model",
					"openai.timeout=30s",
					"ai.summary.client=openai",
					"ai.summary.model-name=summary-model",
					"ai.summary.prompt-version=v1",
					"ai.summary.max-input-chars=250000",
					"ai.summary.active-job-timeout=30m",
					"ai.summary.reasoning-effort=medium",
					"ai.summary.max-output-tokens=8000",
					"ai.summary.timeout=120s"
			);

	@Test
	void 요약은_공통_OpenAI_클라이언트와_별도의_타임아웃_클라이언트를_사용한다() {
		contextRunner.run(context -> {
			assertThat(context).hasBean("openAiRestClient");
			assertThat(context).hasBean("openAiSummaryRestClient");
			assertThat(context).hasBean("openAiSummaryApiClient");
			assertThat(context.getBean("openAiRestClient"))
					.isNotSameAs(context.getBean("openAiSummaryRestClient"));
			assertThat(context.getBean("openAiSummaryApiClient")).isInstanceOf(OpenAiClient.class);
			assertThat(context).hasSingleBean(OpenAiSummaryClient.class);
			assertThat(context.getBean(SummaryProperties.class).timeout()).isEqualTo(Duration.ofSeconds(120));
		});
	}

	@Configuration(proxyBeanMethods = false)
	@Import({OpenAiClientConfig.class, OpenAiSummaryClient.class})
	@EnableConfigurationProperties(SummaryProperties.class)
	static class OpenAiSummaryClientConfiguration {

		@Bean
		RestClient.Builder restClientBuilder() {
			return RestClient.builder();
		}

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}
	}
}
