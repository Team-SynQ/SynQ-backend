package com.synq.backend.domain.reference.link;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 링크 참고자료 fetch 전용 HTTP 클라이언트.
 *
 * <p>Soniox 용 클라이언트(sonioxOkHttpClient)를 재사용하지 않는다. 그쪽은 회의 내내 열려 있는
 * WebSocket 용이라 읽기 타임아웃이 0(무제한)이다. 발화 사이 침묵에 연결이 끊기면 안 되기 때문인데,
 * 그 설정으로 링크를 가져오면 연결만 맺고 데이터를 안 보내는 서버에 스레드가 영원히 묶인다.
 */
@Configuration
public class LinkHttpClientConfig {

	@Bean(name = "linkHttpClient")
	public OkHttpClient linkHttpClient(LinkProperties properties) {
		return new OkHttpClient.Builder()
				// SSRF 방어. 커넥션 직전 해석을 가로채므로 리다이렉트 홉까지 전부 검사된다.
				.dns(new SafeDns(properties.allowPrivateNetwork()))
				.connectTimeout(properties.connectTimeout())
				.readTimeout(properties.readTimeout())
				// connect/read 만으로는 초당 1바이트씩 흘리는 응답에 스레드가 영구 점유된다.
				.callTimeout(properties.callTimeout())
				// 쿠키 저장소를 두지 않는다(기본값). 서버 자격으로 인증된 요청을 대신 보내면 안 된다.
				.followRedirects(true)
				.build();
	}
}
