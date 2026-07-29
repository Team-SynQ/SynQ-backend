package com.synq.backend.domain.ai.summary.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.synq.backend.domain.ai.summary.domain.TranscriptSegment;
import com.synq.backend.domain.ai.summary.mock.MockRagContextReader;
import java.util.List;
import org.junit.jupiter.api.Test;

class SummaryContextBuilderTest {

	@Test
	void 전체_전사와_참고자료를_요약_컨텍스트로_조합한다() {
		var contextBuilder = new SummaryContextBuilder(
				meetingId -> List.of(new TranscriptSegment("SPEAKER_1",
						"회의 후 AI 요약 API를 이번 스프린트에 구현하면 좋겠습니다.")),
				new MockRagContextReader(),
				new SummaryProperties("test-model", "test-v1", 600_000));
		var context = contextBuilder.build(1L);

		assertThat(context.transcript())
				.contains("SPEAKER_1: 회의 후 AI 요약 API를 이번 스프린트에 구현하면 좋겠습니다.");
		assertThat(context.referenceContexts()).hasSize(1);
	}
}
