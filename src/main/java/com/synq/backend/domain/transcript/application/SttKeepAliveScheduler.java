package com.synq.backend.domain.transcript.application;

import com.synq.backend.domain.transcript.ws.SttSession;
import com.synq.backend.domain.transcript.ws.SttSessionRegistry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 전사 연결을 유휴 타임아웃으로부터 지키는 스케줄러. 끊는 주체가 둘이고 방향도 상대도 달라
 * 하나가 다른 하나를 대신하지 못하므로 주기를 따로 둔다.
 *
 * <ul>
 *   <li>서버 → 브라우저 ping: nginx {@code proxy_read_timeout} 과 Tomcat 세션 유휴 타임아웃을 막는다.</li>
 *   <li>서버 → Soniox keepalive: 오디오 20초 공백 시 Soniox 가 스트림을 끊는 것을 막는다.</li>
 * </ul>
 *
 * <p>두 작업 모두 짧고 I/O 만 하므로 세션 수만큼 순회해도 부담이 없다. 다만 {@code @Scheduled}
 * 스레드 풀을 SttIdleFlushScheduler(DB 쓰기 포함)와 공유하므로, 풀 크기가 1이면 여기가 굶어
 * 연결이 그대로 끊긴다. spring.task.scheduling.pool.size 설정이 함께 있어야 한다.
 */
@Component
@RequiredArgsConstructor
public class SttKeepAliveScheduler {

	private static final Logger log = LoggerFactory.getLogger(SttKeepAliveScheduler.class);

	private final SttSessionRegistry registry;

	/** nginx 90초, Tomcat 5분 중 짧은 쪽의 1/3. 한두 번 밀려도 끊기지 않을 여유를 둔다. */
	@Scheduled(fixedDelay = 30_000L)
	public void pingBrowserSessions() {
		try {
			registry.pingAll();
		} catch (RuntimeException e) {
			// 여기서 예외가 새어 나가면 fixedDelay 스케줄이 통째로 멈춰 모든 연결이 끊긴다.
			log.error("WebSocket ping 주기 전송에 실패했습니다.", e);
		}
	}

	/**
	 * keepalive 자체의 유휴 판정은 세션별 마지막 전송 시각으로 하므로, 여기서는 판정 주기만 정한다.
	 * 임계값(기본 10초)보다 촘촘히 돌아야 판정이 한 주기씩 늦어지지 않는다.
	 */
	@Scheduled(fixedDelay = 5_000L)
	public void keepSonioxStreamsAlive() {
		for (SttSession session : registry.activeSessions()) {
			try {
				session.keepSonioxStreamAlive();
			} catch (RuntimeException e) {
				// 한 회의의 실패가 나머지 회의의 스트림까지 죽이지 않도록 격리한다.
				log.warn("Soniox keepalive 전송에 실패했습니다. meetingId={}", session.meetingId(), e);
			}
		}
	}
}
