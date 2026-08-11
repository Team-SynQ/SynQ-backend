package com.synq.backend.domain.transcript.client.soniox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synq.backend.domain.transcript.config.SonioxProperties;
import okhttp3.OkHttpClient;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Soniox 는 오디오도 keepalive 도 20초 이상 없으면 스트림을 끊는다. 마이크 음소거/회의 일시정지처럼
 * 브라우저 연결은 멀쩡한데 오디오만 멎는 구간을 keepalive 가 실제로 메우는지 검증한다.
 */
class SonioxStreamClientTest {

	private static final String KEEPALIVE = "{\"type\":\"keepalive\"}";
	/** 임계값을 0 으로 주면 "유휴 시간이 이미 지난" 상태를 시계 조작 없이 재현할 수 있다. */
	private static final long ELAPSED = 0L;
	private static final long NOT_ELAPSED = 60_000L;

	private final OkHttpClient httpClient = mock(OkHttpClient.class);
	private final WebSocket webSocket = mock(WebSocket.class);
	private final SonioxStreamListener listener = mock(SonioxStreamListener.class);

	private SonioxStreamClient client;

	private static SonioxProperties properties() {
		return new SonioxProperties("test-key", "wss://stt-rt.soniox.com/transcribe-websocket",
				"stt-rt-preview", "auto", List.of("ko"), 5000L, 3000L, 10_000L);
	}

	@BeforeEach
	void setUp() {
		given(httpClient.newWebSocket(any(), any(WebSocketListener.class))).willReturn(webSocket);
		client = new SonioxStreamClient(1L, httpClient, new ObjectMapper(), properties(), listener);
		client.connect();
	}

	/** onOpen 이 config 를 보낸 뒤라야 오디오/keepalive 가 유효하다. */
	private void open() {
		client.onOpen(webSocket, null);
	}

	@Test
	@DisplayName("유휴 시간이 지났으면 keepalive 를 보낸다")
	void sendsKeepaliveWhenIdle() {
		open();

		client.sendKeepaliveIfIdle(ELAPSED);

		verify(webSocket).send(KEEPALIVE);
	}

	@Test
	@DisplayName("유휴 시간이 지나지 않았으면 보내지 않는다")
	void doesNotSendKeepaliveBeforeIdleThreshold() {
		open();

		client.sendKeepaliveIfIdle(NOT_ELAPSED);

		verify(webSocket, never()).send(KEEPALIVE);
	}

	@Test
	@DisplayName("onOpen 전에는 보내지 않는다 - config 보다 먼저 나가면 프로토콜 순서가 깨진다")
	void doesNotSendKeepaliveBeforeOpen() {
		client.sendKeepaliveIfIdle(ELAPSED);

		verify(webSocket, never()).send(KEEPALIVE);
	}

	@Test
	@DisplayName("종료된 스트림에는 보내지 않는다")
	void doesNotSendKeepaliveAfterAbort() {
		open();
		client.abort();

		client.sendKeepaliveIfIdle(ELAPSED);

		verify(webSocket, never()).send(KEEPALIVE);
	}

	@Test
	@DisplayName("오디오를 흘리면 유휴 시계가 리셋돼 keepalive 가 나가지 않는다")
	void audioResetsIdleClock() {
		open();
		client.sendKeepaliveIfIdle(ELAPSED);

		client.sendAudio(new byte[] {1, 2, 3});
		client.sendKeepaliveIfIdle(NOT_ELAPSED);

		// 첫 번째 호출분 1회뿐이어야 한다. 오디오가 시계를 되돌리지 않으면 2회가 된다.
		verify(webSocket).send(KEEPALIVE);
	}
}
