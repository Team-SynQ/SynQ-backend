package com.synq.backend.domain.transcript.ws;

import com.synq.backend.domain.meeting.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 진행자 WS 연결이 끊긴 뒤 유예시간 동안 재연결을 기다렸다가, 그래도 재연결이 없으면 회의를 강제
 * 종료한다. 탭 새로고침/짧은 네트워크 순단으로 인한 순간적인 연결 끊김을 진짜 이탈로 오인하지
 * 않기 위한 유예다.
 */
@Component
@RequiredArgsConstructor
public class HostDisconnectGraceService {

	private static final Logger log = LoggerFactory.getLogger(HostDisconnectGraceService.class);
	private static final Duration GRACE_PERIOD = Duration.ofMinutes(3);

	private final TaskScheduler taskScheduler;
	private final MeetingService meetingService;

	private final Map<Long, ScheduledFuture<?>> pending = new ConcurrentHashMap<>();

	/** 진행자 WS 연결이 끊겼을 때 호출한다. 이미 걸려있던 타이머가 있으면 새 타이머로 교체한다. */
	public void scheduleForceEnd(Long meetingId) {
		ScheduledFuture<?> future = taskScheduler.schedule(
				() -> forceEnd(meetingId), Instant.now().plus(GRACE_PERIOD));
		ScheduledFuture<?> previous = pending.put(meetingId, future);
		if (previous != null) {
			previous.cancel(false);
		}
	}

	/** 진행자가 유예시간 안에 재연결했을 때 호출한다. 걸려있던 타이머가 없어도 안전하다. */
	public void cancel(Long meetingId) {
		ScheduledFuture<?> future = pending.remove(meetingId);
		if (future != null) {
			future.cancel(false);
		}
	}

	private void forceEnd(Long meetingId) {
		pending.remove(meetingId);
		try {
			meetingService.forceEndByDisconnect(meetingId);
			log.info("진행자 연결 끊김으로 회의를 강제 종료했습니다. meetingId={}", meetingId);
		} catch (RuntimeException e) {
			log.error("진행자 연결 끊김에 의한 회의 강제 종료에 실패했습니다. meetingId={}", meetingId, e);
		}
	}
}
