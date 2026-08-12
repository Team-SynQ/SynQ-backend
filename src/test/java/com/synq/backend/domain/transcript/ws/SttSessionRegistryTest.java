package com.synq.backend.domain.transcript.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class SttSessionRegistryTest {

	private final SttSessionRegistry registry = new SttSessionRegistry(new ObjectMapper());

	@Test
	void 등록된_구독자_전원에게_브로드캐스트한다() throws IOException {
		WebSocketSession subscriber1 = mock(WebSocketSession.class);
		WebSocketSession subscriber2 = mock(WebSocketSession.class);
		when(subscriber1.isOpen()).thenReturn(true);
		when(subscriber2.isOpen()).thenReturn(true);
		registry.registerSubscriber(1L, subscriber1);
		registry.registerSubscriber(1L, subscriber2);

		registry.broadcastToSubscribers(1L, SttServerMessage.transcript(10L, "안녕", 0, 0));

		verify(subscriber1).sendMessage(any(TextMessage.class));
		verify(subscriber2).sendMessage(any(TextMessage.class));
	}

	@Test
	void 제거된_구독자에게는_보내지_않는다() throws IOException {
		WebSocketSession subscriber = mock(WebSocketSession.class);
		when(subscriber.isOpen()).thenReturn(true);
		registry.registerSubscriber(1L, subscriber);
		registry.removeSubscriber(1L, subscriber);

		registry.broadcastToSubscribers(1L, SttServerMessage.transcript(10L, "안녕", 0, 0));

		verify(subscriber, never()).sendMessage(any(TextMessage.class));
	}

	@Test
	void 닫힌_세션에는_보내지_않는다() throws IOException {
		WebSocketSession subscriber = mock(WebSocketSession.class);
		when(subscriber.isOpen()).thenReturn(false);
		registry.registerSubscriber(1L, subscriber);

		registry.broadcastToSubscribers(1L, SttServerMessage.transcript(10L, "안녕", 0, 0));

		verify(subscriber, never()).sendMessage(any(TextMessage.class));
	}

	@Test
	void 다른_회의의_구독자에게는_보내지_않는다() throws IOException {
		WebSocketSession subscriber = mock(WebSocketSession.class);
		when(subscriber.isOpen()).thenReturn(true);
		registry.registerSubscriber(2L, subscriber);

		registry.broadcastToSubscribers(1L, SttServerMessage.transcript(10L, "안녕", 0, 0));

		verify(subscriber, never()).sendMessage(any(TextMessage.class));
	}

	@Test
	void 구독자가_없는_회의는_예외없이_조용히_지나간다() {
		registry.broadcastToSubscribers(999L, SttServerMessage.transcript(10L, "안녕", 0, 0));
	}

	// 호스트 세션(Map<Long, SttSession>)과 구독자 목록(Map<Long, Set<WebSocketSession>>)은 독립적인 맵이라,
	// 호스트 세션 등록/제거가 구독자 목록에 영향을 주면 안 된다.
	@Test
	void 호스트_세션_등록_제거는_구독자_목록에_영향을_주지_않는다() throws IOException {
		SttSession hostSession = mock(SttSession.class);
		when(hostSession.meetingId()).thenReturn(1L);
		WebSocketSession subscriber = mock(WebSocketSession.class);
		when(subscriber.isOpen()).thenReturn(true);
		registry.registerSubscriber(1L, subscriber);

		registry.register(hostSession);
		registry.remove(hostSession);
		registry.broadcastToSubscribers(1L, SttServerMessage.transcript(10L, "안녕", 0, 0));

		verify(subscriber).sendMessage(any(TextMessage.class));
	}

	@Test
	void 회의_종료시_열려있는_구독자_연결을_서버가_닫는다() throws IOException {
		WebSocketSession subscriber = mock(WebSocketSession.class);
		when(subscriber.isOpen()).thenReturn(true);
		registry.registerSubscriber(1L, subscriber);

		registry.closeSubscribers(1L);

		verify(subscriber).close(CloseStatus.NORMAL);
	}

	@Test
	void 회의_종료시_이미_닫힌_구독자_연결은_다시_닫지_않는다() throws IOException {
		WebSocketSession subscriber = mock(WebSocketSession.class);
		when(subscriber.isOpen()).thenReturn(false);
		registry.registerSubscriber(1L, subscriber);

		registry.closeSubscribers(1L);

		verify(subscriber, never()).close(any(CloseStatus.class));
	}

	@Test
	void 구독자가_없는_회의를_종료해도_예외없이_지나간다() {
		registry.closeSubscribers(999L);
	}

	@Test
	void 구독자_정리_후에는_같은_회의로_브로드캐스트해도_아무도_받지_않는다() throws IOException {
		WebSocketSession subscriber = mock(WebSocketSession.class);
		when(subscriber.isOpen()).thenReturn(true);
		registry.registerSubscriber(1L, subscriber);

		registry.closeSubscribers(1L);
		registry.broadcastToSubscribers(1L, SttServerMessage.transcript(10L, "안녕", 0, 0));

		verify(subscriber, never()).sendMessage(any(TextMessage.class));
	}
}
