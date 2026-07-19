package com.synq.backend.domain.ai.summary.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.synq.backend.domain.ai.summary.domain.TranscriptSegment;
import com.synq.backend.domain.ai.summary.mock.MockMeetingContextReader;
import com.synq.backend.domain.ai.summary.mock.MockRagContextReader;
import java.util.List;
import org.junit.jupiter.api.Test;

class SummaryContextBuilderTest {

	@Test
	void 전사와_회의_맥락과_참고자료를_요약_컨텍스트로_조합한다() {
		var contextBuilder = new SummaryContextBuilder(
				meetingId -> List.of(new TranscriptSegment("SPEAKER_1",
						"회의 후 AI 요약 API를 이번 스프린트에 구현하면 좋겠습니다.")),
				new MockMeetingContextReader(), new MockRagContextReader());
		var context = contextBuilder.build(1L);

		assertThat(context.transcript())
				.contains("회의 후 AI 요약 API를 이번 스프린트에 구현하면 좋겠습니다.")
				.doesNotContain("SPEAKER_1");
		assertThat(context.rollingSummary()).contains("회의 후 AI 요약");
		assertThat(context.referenceContexts()).hasSize(1);
	}
}
