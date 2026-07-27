package com.synq.backend.domain.transcript.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synq.backend.domain.transcript.client.soniox.SonioxStreamClient;
import com.synq.backend.domain.transcript.client.soniox.SonioxStreamListener;
import com.synq.backend.domain.transcript.config.SonioxProperties;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/** 세션마다 Soniox 클라이언트를 새로 만든다. 테스트에서 가짜 구현으로 갈아끼우는 지점이기도 하다. */
@Component
@RequiredArgsConstructor
public class SonioxClientFactory {

	@Qualifier("sonioxOkHttpClient")
	private final OkHttpClient okHttpClient;
	private final ObjectMapper objectMapper;
	private final SonioxProperties properties;

	public SonioxStreamClient create(Long meetingId, SonioxStreamListener listener) {
		return new SonioxStreamClient(meetingId, okHttpClient, objectMapper, properties, listener);
	}
}
