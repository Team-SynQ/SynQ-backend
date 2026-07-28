package com.synq.backend.domain.ai.event;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SSE 연결 만료와 하트비트 주기를 외부 설정으로 관리한다.
 */
@ConfigurationProperties(prefix = "ai.event.sse")
public record AiEventSseProperties(
		Duration timeout,
		Duration heartbeatInterval
) {

	public AiEventSseProperties {
		if (timeout == null || timeout.isNegative() || timeout.isZero()) {
			throw new IllegalArgumentException("SSE timeout은 0보다 커야 합니다.");
		}
		if (heartbeatInterval == null || heartbeatInterval.isNegative() || heartbeatInterval.isZero()) {
			throw new IllegalArgumentException("SSE heartbeatInterval은 0보다 커야 합니다.");
		}
	}
}
