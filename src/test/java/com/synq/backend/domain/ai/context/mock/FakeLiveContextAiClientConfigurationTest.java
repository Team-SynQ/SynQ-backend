package com.synq.backend.domain.ai.context.mock;

import static org.assertj.core.api.Assertions.assertThat;

import com.synq.backend.domain.ai.client.openai.OpenAiLiveContextClient;
import com.synq.backend.domain.ai.context.domain.LiveContextAiClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class FakeLiveContextAiClientConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(FakeLiveContextAiClientConfiguration.class);

	@Test
	void prod_프로필에서도_fake_클라이언트_설정이면_빈으로_등록된다() {
		contextRunner
				.withPropertyValues(
						"spring.profiles.active=prod",
						"ai.live-context.client=fake"
				)
				.run(context -> assertThat(context).hasSingleBean(LiveContextAiClient.class));
	}

	@Configuration(proxyBeanMethods = false)
	@Import({FakeLiveContextAiClient.class, OpenAiLiveContextClient.class})
	static class FakeLiveContextAiClientConfiguration {
	}
}
