package com.synq.backend.domain.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.synq.backend.domain.ai.client.openai.OpenAiSummaryClient;
import com.synq.backend.domain.ai.rag.mock.FakeReferenceMaterialPort;
import com.synq.backend.domain.ai.rag.port.ReferenceMaterialPort;
import com.synq.backend.domain.ai.summary.domain.MeetingContextReader;
import com.synq.backend.domain.ai.summary.domain.MeetingSummaryStore;
import com.synq.backend.domain.ai.summary.domain.RagContextReader;
import com.synq.backend.domain.ai.summary.domain.SummaryAiClient;
import com.synq.backend.domain.ai.summary.domain.SummaryJobStore;
import com.synq.backend.domain.ai.summary.domain.TranscriptReader;
import com.synq.backend.domain.ai.summary.mock.FakeSummaryAiClient;
import com.synq.backend.domain.ai.summary.mock.InMemoryMeetingSummaryStore;
import com.synq.backend.domain.ai.summary.mock.InMemorySummaryJobStore;
import com.synq.backend.domain.ai.summary.mock.MockMeetingContextReader;
import com.synq.backend.domain.ai.summary.mock.MockRagContextReader;
import com.synq.backend.domain.ai.summary.mock.MockTranscriptReader;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class ProdAiMockConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(ProdAiMockConfiguration.class)
			.withPropertyValues(
					"spring.profiles.active=prod",
					"ai.rag.reference-material.client=fake",
					"ai.summary.client=fake",
					"ai.summary.context-source=mock"
			);

	@Test
	void prod_프로필에서_fake_mock_설정에_맞는_AI_대역을_등록한다() {
		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(ReferenceMaterialPort.class);
			assertThat(context).hasSingleBean(SummaryAiClient.class);
			assertThat(context).hasSingleBean(SummaryJobStore.class);
			assertThat(context).hasSingleBean(MeetingSummaryStore.class);
			assertThat(context).hasSingleBean(TranscriptReader.class);
			assertThat(context).hasSingleBean(MeetingContextReader.class);
			assertThat(context).hasSingleBean(RagContextReader.class);
			assertThat(context.getBean(SummaryAiClient.class)).isInstanceOf(FakeSummaryAiClient.class);
		});
	}

	@Configuration(proxyBeanMethods = false)
	@Import({
			FakeReferenceMaterialPort.class,
			FakeSummaryAiClient.class,
			OpenAiSummaryClient.class,
			InMemorySummaryJobStore.class,
			InMemoryMeetingSummaryStore.class,
			MockTranscriptReader.class,
			MockMeetingContextReader.class,
			MockRagContextReader.class
	})
	static class ProdAiMockConfiguration {
	}
}
