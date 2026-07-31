package com.synq.backend.domain.ai.assistant.domain;

/**
 * AI 채팅 답변을 생성하는 제공자 포트다.
 */
public interface AiChatClient {

	AiChatResult generate(AiChatPrompt prompt);

	AiChatWelcome generateWelcome(AiChatContext context);
}
