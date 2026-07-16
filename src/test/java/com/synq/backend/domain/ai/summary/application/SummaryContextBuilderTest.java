package com.synq.backend.domain.ai.summary.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.synq.backend.domain.ai.summary.mock.MockMeetingContextReader;
import com.synq.backend.domain.ai.summary.mock.MockRagContextReader;
import com.synq.backend.domain.ai.summary.mock.MockTranscriptReader;
import org.junit.jupiter.api.Test;

class SummaryContextBuilderTest {

	private final SummaryContextBuilder contextBuilder = new SummaryContextBuilder(
			new MockTranscriptReader(), new MockMeetingContextReader(), new MockRagContextReader());

	@Test
	void 전사와_회의_맥락과_참고자료를_요약_컨텍스트로_조합한다() {
		var context = contextBuilder.build(1L);

		assertThat(context.transcript()).contains("민규:").contains("현규:");
		assertThat(context.rollingSummary()).contains("회의 후 AI 요약");
		assertThat(context.referenceContexts()).hasSize(1);
	}
}
