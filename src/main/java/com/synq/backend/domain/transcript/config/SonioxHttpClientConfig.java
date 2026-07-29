package com.synq.backend.domain.transcript.config;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * OkHttpClient 빈을 별도 설정 클래스로 분리한다.
 * TranscriptWebSocketConfig 안에 두면 그 설정이 주입받는 HostAudioWebSocketHandler → SonioxClientFactory
 * 가 다시 이 빈을 요구해 TranscriptWebSocketConfig ↔ HostAudioWebSocketHandler 순환 참조가 생긴다.
 */
@Configuration
public class SonioxHttpClientConfig {

	@Bean(name = "sonioxOkHttpClient")
	public OkHttpClient sonioxOkHttpClient() {
		return new OkHttpClient.Builder()
				.connectTimeout(5, TimeUnit.SECONDS)
				// 실시간 스트림은 오래 열려 있어야 하므로 읽기 타임아웃을 두지 않는다.
				.readTimeout(0, TimeUnit.MILLISECONDS)
				.pingInterval(20, TimeUnit.SECONDS)
				.build();
	}
}
