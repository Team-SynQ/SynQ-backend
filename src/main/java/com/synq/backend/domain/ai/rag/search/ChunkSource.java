package com.synq.backend.domain.ai.rag.search;

/**
 * 청크가 어디서 왔는지 구분한다.
 *
 * 이름이 그대로 프롬프트 라벨로 나가므로(OpenAiChatClient) 값을 바꾸면 LLM 이 보는
 * 근거 표기가 함께 바뀐다.
 */
public enum ChunkSource {
	REFERENCE_MATERIAL,
	MEETING_TRANSCRIPT
}
