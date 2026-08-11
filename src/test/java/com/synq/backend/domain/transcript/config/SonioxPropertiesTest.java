package com.synq.backend.domain.transcript.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class SonioxPropertiesTest {

	private static SonioxProperties withKeepaliveIdleMs(long keepaliveIdleMs) {
		return new SonioxProperties("key", "wss://example.com", "stt-rt-preview", "auto",
				List.of("ko"), 5000L, 3000L, keepaliveIdleMs);
	}

	@Test
	@DisplayName("keepalive 주기가 Soniox 의 20초 상한 이상이면 거부한다")
	void rejectsKeepaliveIdleAtOrAboveSonioxLimit() {
		// 상한 이상으로 잡으면 keepalive 가 나가기 전에 Soniox 가 먼저 스트림을 끊어 설정이 무의미해진다.
		assertThatIllegalArgumentException().isThrownBy(() -> withKeepaliveIdleMs(20_000L));
		assertThatIllegalArgumentException().isThrownBy(() -> withKeepaliveIdleMs(30_000L));
		assertThatIllegalArgumentException().isThrownBy(() -> withKeepaliveIdleMs(0L));
	}

	@Test
	@DisplayName("상한 미만이면 허용한다")
	void acceptsKeepaliveIdleBelowSonioxLimit() {
		assertThatCode(() -> withKeepaliveIdleMs(10_000L)).doesNotThrowAnyException();
		assertThatCode(() -> withKeepaliveIdleMs(19_999L)).doesNotThrowAnyException();
	}
}
