package com.synq.backend.domain.meeting.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

// nginx가 X-Forwarded-Proto를 보내고 server.forward-headers-strategy=framework가 켜져 있어,
// request.isSecure()가 운영(https 뒤)에서는 true, 로컬(http)에서는 false로 정확히 잡힌다.
// 그래서 도메인/scheme을 설정값으로 하드코딩하지 않고 요청 자체에서 읽어 ws/wss를 결정한다.
@Component
public class MeetingWsUrlResolver {

	public String resolve(HttpServletRequest request, Long meetingId) {
		boolean secure = request.isSecure();
		String scheme = secure ? "wss" : "ws";
		int port = request.getServerPort();

		UriComponentsBuilder builder = ServletUriComponentsBuilder.fromContextPath(request)
				.scheme(scheme)
				.replacePath("/ws/meetings/{meetingId}/stt");
		// scheme 을 ws/wss 로 바꿔치기해도 원래 요청의 port(443/80)가 그대로 남으므로,
		// 새 scheme 기준 기본 포트면 명시적으로 지워서 wss://host:443 같은 군더더기를 없앤다.
		if ((secure && port == 443) || (!secure && port == 80)) {
			// UriComponentsBuilder 규약상 -1 이 "포트 미지정"을 뜻한다.
			builder.port(-1);
		}
		return builder.buildAndExpand(meetingId).toUriString();
	}
}
