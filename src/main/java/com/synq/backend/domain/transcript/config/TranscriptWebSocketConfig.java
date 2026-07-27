package com.synq.backend.domain.transcript.config;

import com.synq.backend.domain.transcript.ws.HostAudioWebSocketHandler;
import com.synq.backend.domain.transcript.ws.SttHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import java.util.List;

/** 호스트 오디오 수신용 raw WebSocket 설정. STOMP 는 오디오 릴레이에 불필요한 오버헤드라 쓰지 않는다. */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class TranscriptWebSocketConfig implements WebSocketConfigurer {

	private final HostAudioWebSocketHandler hostAudioWebSocketHandler;
	private final SttHandshakeInterceptor sttHandshakeInterceptor;

	@Value("${cors.allowed-origins}")
	private List<String> allowedOrigins;

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(hostAudioWebSocketHandler, "/ws/meetings/{meetingId}/stt")
				.addInterceptors(sttHandshakeInterceptor)
				.setAllowedOriginPatterns(allowedOrigins.toArray(String[]::new));
	}

	@Bean
	public ServletServerContainerFactoryBean createWebSocketContainer() {
		// @SpringBootTest 의 기본 MOCK 웹 환경은 MockServletContext 를 쓰는데, 여기엔 JSR-356
		// ServerContainer 속성이 없어 afterPropertiesSet() 이 항상 예외를 던진다. 실제 배포(임베디드
		// Tomcat)에서는 onRefresh() 단계에서 이미 그 속성이 채워져 있어 정상 동작하므로, 그 예외만 무시한다.
		ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean() {
			@Override
			public void afterPropertiesSet() {
				try {
					super.afterPropertiesSet();
				} catch (IllegalStateException e) {
					// mock 웹 환경에서는 버퍼 크기 설정이 의미가 없으므로 무시하고 넘어간다.
				}
			}
		};
		// 기본 8KB 는 MediaRecorder 의 1 초 오디오 청크에 너무 작다. 넘으면 연결이 끊긴다.
		container.setMaxBinaryMessageBufferSize(512 * 1024);
		container.setMaxTextMessageBufferSize(64 * 1024);
		// 호스트가 말을 하지 않아도 오디오는 계속 흐르지만, 유휴 상태에서 조기에 끊기지 않게 넉넉히 잡는다.
		container.setMaxSessionIdleTimeout(300_000L);
		return container;
	}
}
