package com.synq.backend.domain.ai.context.mock;

import com.synq.backend.domain.ai.context.domain.LiveContextAiClient;
import com.synq.backend.domain.ai.context.domain.LiveContextResult;
import com.synq.backend.domain.ai.context.domain.LiveContextSnapshot;
import com.synq.backend.domain.transcript.event.TranscriptFinalizedEvent;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * {@code ai.live-context.client=fake}일 때 사용하는 Live Context AI 대역이다.
 *
 * <p>API 키 없이 전사 이벤트부터 영속 상태 갱신까지 검증하거나, 실제 AI 연동 전 운영 환경을 기동할 때 사용한다.
 */
@Component
@ConditionalOnProperty(prefix = "ai.live-context", name = "client", havingValue = "fake", matchIfMissing = true)
public class FakeLiveContextAiClient implements LiveContextAiClient {

	@Override
	public LiveContextResult refresh(LiveContextSnapshot previousContext, TranscriptFinalizedEvent event) {
		String summary = previousContext.rollingSummary().isBlank()
				? event.content()
				: previousContext.rollingSummary() + " " + event.content();

		return new LiveContextResult(
				summary,
				"회의 진행 사항",
				previousContext.decisions(),
				previousContext.actionItems(),
				List.of("다음 논의 항목을 확인한다.")
		);
	}
}
