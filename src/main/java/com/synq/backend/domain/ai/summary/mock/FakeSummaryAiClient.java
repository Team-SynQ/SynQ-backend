package com.synq.backend.domain.ai.summary.mock;

import com.synq.backend.domain.ai.summary.domain.GeneratedSummary;
import com.synq.backend.domain.ai.summary.domain.GeneratedPersonalSummary;
import com.synq.backend.domain.ai.summary.domain.PersonalSummaryAiClient;
import com.synq.backend.domain.ai.summary.domain.PersonalSummaryTarget;
import com.synq.backend.domain.ai.summary.domain.SummaryContext;
import com.synq.backend.domain.ai.summary.domain.SummaryAiClient;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "ai.summary", name = "client", havingValue = "fake")
public class FakeSummaryAiClient implements SummaryAiClient, PersonalSummaryAiClient {

	@Override
	public GeneratedSummary generate(SummaryContext context) {
		// 개발·테스트 환경에서는 비용과 API 키 없이 응답 형식과 Job 흐름을 검증한다.
		return new GeneratedSummary(
				"회의 후 AI 요약 API를 우선 구현하고 전사와 참고자료를 함께 활용하기로 했습니다.",
				List.of("회의 후 AI 요약", "전사와 참고자료 활용", "SSE 적용 시점"),
				List.of(new com.synq.backend.domain.ai.summary.domain.DiscussionSection(
						"회의 후 AI 요약 구현",
						List.of("전사와 참고자료를 요약 입력으로 활용한다.", "기본 API 흐름을 이번 스프린트에 구현한다.")
				)),
				List.of("이번 스프린트에서 회의 후 AI 요약 API 기본 흐름을 구현한다."),
				List.of("SSE 적용 시점은 다음 회의에서 결정한다."),
				List.of("API 명세 초안을 작성한다.")
		);
	}

	@Override
	public GeneratedPersonalSummary generate(
			SummaryContext context,
			GeneratedSummary overallSummary,
			PersonalSummaryTarget target
	) {
		return new GeneratedPersonalSummary(
				target.roleDescription() + " 관점에서 회의 결과와 후속 작업을 확인해야 합니다.",
				List.of("회의 후 AI 요약 API 구현", "전사 데이터 연동"),
				List.of(),
				List.of("내 역할에서 추가로 확인할 위험 요소는 무엇인가?")
		);
	}
}
