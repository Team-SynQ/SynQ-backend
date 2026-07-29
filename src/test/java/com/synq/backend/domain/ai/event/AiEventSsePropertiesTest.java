package com.synq.backend.domain.ai.event;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class AiEventSsePropertiesTest {

	@Test
	void 하트비트_주기는_연결_만료_시간보다_짧아야_한다() {
		assertThatThrownBy(() -> new AiEventSseProperties(
				Duration.ofSeconds(20), Duration.ofSeconds(20), 100
		)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("SSE heartbeatInterval은 timeout보다 짧아야 합니다.");
	}

	@Test
	void 연결별_대기열_크기는_1_이상이어야_한다() {
		assertThatThrownBy(() -> new AiEventSseProperties(
				Duration.ofMinutes(1), Duration.ofSeconds(20), 0
		)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("SSE queueCapacity는 1 이상이어야 합니다.");
	}
}
