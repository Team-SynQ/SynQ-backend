package com.synq.backend.domain.ai.summary.mock;

import com.synq.backend.domain.ai.summary.domain.GeneratedSummary;
import com.synq.backend.domain.ai.summary.domain.SummaryContext;
import com.synq.backend.domain.ai.summary.domain.SummaryAiClient;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "test"})
@ConditionalOnProperty(prefix = "ai.summary", name = "client", havingValue = "fake", matchIfMissing = true)
public class FakeSummaryAiClient implements SummaryAiClient {

	@Override
	public GeneratedSummary generate(SummaryContext context) {
		// 개발·테스트 환경에서는 비용과 API 키 없이 응답 형식과 Job 흐름을 검증한다.
		return new GeneratedSummary(
				"회의 후 AI 요약 API를 우선 구현하고, 전사와 참고자료를 함께 활용하기로 했습니다.",
				List.of("회의 후 AI 요약", "전사와 참고자료 활용", "SSE 적용 시점"),
				List.of("이번 스프린트에서 회의 후 AI 요약 API 기본 흐름을 구현한다."),
				List.of("민규: API 명세 초안을 작성한다."),
				List.of("SSE 적용 시점을 다음 회의에서 결정한다.")
		);
	}
}
