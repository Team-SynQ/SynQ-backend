package com.synq.backend.domain.ai.assistant.mock;

import com.synq.backend.domain.ai.assistant.domain.HintAiClient;
import com.synq.backend.domain.ai.assistant.domain.HintInput;
import com.synq.backend.domain.ai.assistant.domain.HintResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "ai.assistant", name = "client", havingValue = "fake")
public class FakeHintAiClient implements HintAiClient {

	@Override
	public HintResult generate(HintInput input) {
		// 비용·키 없이 형식과 흐름을 검증한다. 클릭한 발화를 되짚어 결정성을 준다.
		return new HintResult(
				"이 발화는 \"%s\" 를 뜻합니다.".formatted(input.focusSegment()),
				"%s 관점에서는 검토가 필요합니다.".formatted(
						input.role().isBlank() ? "당신의" : input.role()),
				"이 시점에 팀에게 우선순위를 물어보면 좋겠습니다."
		);
	}
}
