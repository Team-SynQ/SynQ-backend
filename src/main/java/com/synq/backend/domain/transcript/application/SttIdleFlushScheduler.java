package com.synq.backend.domain.transcript.application;

import com.synq.backend.domain.transcript.ws.SttSession;
import com.synq.backend.domain.transcript.ws.SttSessionRegistry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * {@code <end>} 토큰이 오지 않을 때 세그먼트가 영영 확정되지 않는 것을 막는 안전망.
 *
 * <p>세션마다 타이머를 두지 않고 단일 스케줄러가 전체를 훑는다. 회의 수가 수천 개가 되기 전까지는
 * 이쪽이 단순하고 싸다. 강제 확정이 자주 발생하면 {@code <end>} 기반 주 경로가 동작하지 않는다는
 * 신호이므로 SttSession 이 그때마다 로그를 남긴다.
 */
@Component
@RequiredArgsConstructor
public class SttIdleFlushScheduler {

	private static final Logger log = LoggerFactory.getLogger(SttIdleFlushScheduler.class);

	private final SttSessionRegistry registry;

	@Scheduled(fixedDelay = 1000L)
	public void flushIdleBuffers() {
		Instant now = Instant.now();
		for (SttSession session : registry.activeSessions()) {
			try {
				session.flushIfIdle(now);
			} catch (RuntimeException e) {
				// 한 세션의 실패가 다른 회의의 flush 를 막지 않도록 격리한다.
				log.error("유휴 세그먼트 flush 에 실패했습니다. meetingId={}", session.meetingId(), e);
			}
		}
	}
}
