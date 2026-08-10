package com.synq.backend.domain.meeting.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class MeetingWsUrlResolverTest {

	private final MeetingWsUrlResolver resolver = new MeetingWsUrlResolver();

	@Test
	void http_요청이면_ws_스킴에_포트를_유지한_절대_URL을_만든다() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/projects/1/meetings");
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8080);
		request.setSecure(false);

		String wsUrl = resolver.resolve(request, 7L);

		assertThat(wsUrl).isEqualTo("ws://localhost:8080/ws/meetings/7/stt");
	}

	@Test
	void https_요청이면_wss_스킴이고_기본포트_443은_생략된다() {
		// forward-headers-strategy=framework 환경에서 nginx가 X-Forwarded-Proto: https 를 보내면
		// Spring 이 request.isSecure()=true, getServerPort()=443 으로 채운다.
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/projects/1/meetings");
		request.setScheme("https");
		request.setServerName("synqai.co.kr");
		request.setServerPort(443);
		request.setSecure(true);

		String wsUrl = resolver.resolve(request, 7L);

		assertThat(wsUrl).isEqualTo("wss://synqai.co.kr/ws/meetings/7/stt");
	}

	@Test
	void https인데_표준포트가_아니면_포트를_그대로_유지한다() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/projects/1/meetings");
		request.setScheme("https");
		request.setServerName("synqai.co.kr");
		request.setServerPort(8443);
		request.setSecure(true);

		String wsUrl = resolver.resolve(request, 7L);

		assertThat(wsUrl).isEqualTo("wss://synqai.co.kr:8443/ws/meetings/7/stt");
	}
}
