package com.synq.backend.domain.transcript.ws;

import com.synq.backend.domain.transcript.code.TranscriptErrorCode;
import com.synq.backend.global.apipayload.exception.GeneralException;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Soniox 스트림 기준 타임스탬프를 회의 절대 시각으로 변환한다.
 *
 * <p>Soniox 의 start_ms 는 <b>그 연결의 스트림 시작 기준</b>이라 호스트가 재연결하면 0 으로 리셋된다.
 * 그대로 저장하면 재연결 이후 세그먼트가 앞으로 튀어나와 회의록 순서가 뒤집힌다.
 * meeting.startedAt 을 0 점으로 삼고 스트림 시작 시점의 오프셋을 더해 해결한다.
 *
 * <p>이전 스트림의 max endMs 를 이어붙이는 방식과 달리, 끊겨 있던 구간이 공백으로 정확히 남고
 * 인메모리 상태에 의존하지 않아 서버가 재시작돼도 startedAt 만으로 복원된다.
 */
public record MeetingTimeline(long baseOffsetMs) {

	public MeetingTimeline {
		if (baseOffsetMs < 0) {
			throw new IllegalArgumentException("baseOffsetMs 는 0 이상이어야 합니다: " + baseOffsetMs);
		}
	}

	/** 스트림을 여는 시점에 한 번 계산하고 이후 불변으로 쓴다. */
	public static MeetingTimeline from(LocalDateTime meetingStartedAt, LocalDateTime streamStartedAt) {
		if (meetingStartedAt == null) {
			return new MeetingTimeline(0L);
		}
		long offset = Duration.between(meetingStartedAt, streamStartedAt).toMillis();
		// 서버 시계 역행이나 startedAt 이 미래인 경우에도 음수 오프셋을 만들지 않는다.
		return new MeetingTimeline(Math.max(0L, offset));
	}

	public int toAbsoluteMs(int streamRelativeMs) {
		long absolute = baseOffsetMs + streamRelativeMs;
		// start_ms 는 INTEGER 컬럼이다. 조용히 wrap 되지 않도록 명시적으로 막는다.
		if (absolute < 0 || absolute > Integer.MAX_VALUE) {
			throw new GeneralException(TranscriptErrorCode.TIMESTAMP_OUT_OF_RANGE);
		}
		return (int) absolute;
	}
}
