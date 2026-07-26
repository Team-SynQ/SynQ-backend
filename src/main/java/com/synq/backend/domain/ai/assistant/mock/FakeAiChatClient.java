package com.synq.backend.domain.ai.assistant.mock;

import com.synq.backend.domain.ai.assistant.domain.AiChatClient;
import com.synq.backend.domain.ai.assistant.domain.AiChatPrompt;
import com.synq.backend.domain.ai.assistant.domain.AiChatResult;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * API 키 없이 AI Chat 흐름을 검증할 때 사용하는 대역이다.
 */
@Component
@ConditionalOnProperty(prefix = "ai.chat", name = "client", havingValue = "fake")
public class FakeAiChatClient implements AiChatClient {

	@Override
	public AiChatResult generate(AiChatPrompt prompt) {
		return new AiChatResult(
				"현재는 기본 AI Chat 연결을 검증하는 응답입니다. 질문: " + prompt.question(),
				List.of(),
				List.of("이 내용의 핵심 결정은 무엇인가요?", "다음으로 확인할 사항은 무엇인가요?")
		);
	}
}
