package com.synq.backend.domain.transcript.config;

import com.synq.backend.domain.transcript.ws.HostAudioWebSocketHandler;
import com.synq.backend.domain.transcript.ws.SttHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
			private static final Logger log = LoggerFactory.getLogger(TranscriptWebSocketConfig.class);

			@Override
			public void afterPropertiesSet() {
				try {
					super.afterPropertiesSet();
				} catch (IllegalStateException e) {
					// mock 웹 환경(테스트)에서는 항상 이 경로를 타서 기대된 상황이지만, 실제 배포에서
					// 발생하면 WS 버퍼 크기 설정이 통째로 무시된 채 조용히 넘어가는 것이므로 반드시 남긴다.
					log.warn("WebSocket 컨테이너 버퍼 크기 설정을 건너뜁니다 (mock 웹 환경이면 정상, 그 외 환경이면 확인 필요): {}",
							e.getMessage());
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
