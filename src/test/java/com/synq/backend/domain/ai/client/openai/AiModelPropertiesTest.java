package com.synq.backend.domain.ai.client.openai;

import static org.assertj.core.api.Assertions.assertThat;

import com.synq.backend.domain.ai.assistant.application.AiChatProperties;
import com.synq.backend.domain.ai.assistant.service.AssistantAiProperties;
import com.synq.backend.domain.ai.context.application.LiveContextAiProperties;
import com.synq.backend.domain.ai.summary.application.SummaryProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class AiModelPropertiesTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(PropertiesConfiguration.class)
			.withPropertyValues(
					"ai.chat.model=chat-model",
					"ai.chat.reasoning-effort=low",
					"ai.chat.max-output-tokens=2000",
					"ai.chat.recent-transcript-limit=12",
					"ai.chat.linked-segment-window=2",
					"ai.chat.recent-turn-limit=5",
					"ai.chat.rag-top-k=5",
					"ai.chat.rag-min-similarity=0.5",
					"ai.assistant.model=hint-model",
					"ai.assistant.reasoning-effort=low",
					"ai.assistant.max-output-tokens=1200",
					"ai.live-context.model=context-model",
					"ai.live-context.reasoning-effort=low",
					"ai.live-context.max-output-tokens=1200",
					"ai.summary.model-name=summary-model",
					"ai.summary.prompt-version=v1",
					"ai.summary.max-input-chars=250000",
					"ai.summary.active-job-timeout=30m",
					"ai.summary.reasoning-effort=medium",
					"ai.summary.max-output-tokens=8000"
			);

	@Test
	void 기능별_모델_설정이_각_프로퍼티로_분리되어_바인딩된다() {
		contextRunner.run(context -> {
			assertThat(context.getBean(AiChatProperties.class))
				.extracting(AiChatProperties::model, AiChatProperties::reasoningEffort, AiChatProperties::maxOutputTokens)
				.containsExactly("chat-model", "low", 2_000);
			assertThat(context.getBean(AssistantAiProperties.class))
					.extracting(AssistantAiProperties::model, AssistantAiProperties::reasoningEffort,
							AssistantAiProperties::maxOutputTokens)
					.containsExactly("hint-model", "low", 1_200);
			assertThat(context.getBean(LiveContextAiProperties.class))
					.extracting(LiveContextAiProperties::model, LiveContextAiProperties::reasoningEffort,
							LiveContextAiProperties::maxOutputTokens)
					.containsExactly("context-model", "low", 1_200);
			assertThat(context.getBean(SummaryProperties.class))
				.extracting(SummaryProperties::modelName, SummaryProperties::reasoningEffort, SummaryProperties::maxOutputTokens)
				.containsExactly("summary-model", "medium", 8_000);
		});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties({
			AiChatProperties.class,
			AssistantAiProperties.class,
			LiveContextAiProperties.class,
			SummaryProperties.class
	})
	static class PropertiesConfiguration {
	}
}
