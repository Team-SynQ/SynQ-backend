package com.synq.backend.domain.ai.event;

/**
 * 브라우저가 SSE event 이름으로 구분하는 AI 결과 유형이다.
 */
public enum AiEventType {

	CONNECTED("connected"),
	HEARTBEAT("heartbeat"),
	LIVE_CONTEXT_UPDATED("live-context.updated"),
	AUTO_HINT_CREATED("hint.auto-created"),
	SUMMARY_COMPLETED("summary.completed"),
	SUMMARY_FAILED("summary.failed");

	private final String eventName;

	AiEventType(String eventName) {
		this.eventName = eventName;
	}

	public String eventName() {
		return eventName;
	}
}
