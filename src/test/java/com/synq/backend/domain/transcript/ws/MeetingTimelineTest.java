package com.synq.backend.domain.transcript.ws;

import com.synq.backend.global.apipayload.exception.GeneralException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MeetingTimelineTest {

	private static final LocalDateTime MEETING_STARTED_AT = LocalDateTime.of(2026, 7, 27, 10, 0, 0);

	@Test
	@DisplayName("회의 시작과 동시에 스트림이 열리면 Soniox 타임스탬프를 그대로 쓴다")
	void noOffsetWhenStreamStartsWithMeeting() {
		MeetingTimeline timeline = MeetingTimeline.from(MEETING_STARTED_AT, MEETING_STARTED_AT);

		assertThat(timeline.toAbsoluteMs(1500)).isEqualTo(1500);
	}

	@Test
	@DisplayName("재연결로 스트림이 늦게 열리면 그만큼 오프셋이 더해져 이전 세그먼트와 겹치지 않는다")
	void addsOffsetForReconnectedStream() {
		// 회의 시작 2분 30초 뒤에 재연결 → Soniox 는 다시 0ms 부터 준다.
		MeetingTimeline timeline = MeetingTimeline.from(MEETING_STARTED_AT, MEETING_STARTED_AT.plusSeconds(150));

		assertThat(timeline.toAbsoluteMs(0)).isEqualTo(150_000);
		assertThat(timeline.toAbsoluteMs(60_000)).isEqualTo(210_000);
	}

	@Test
	@DisplayName("끊겨 있던 구간이 압축되지 않고 공백으로 남는다")
	void preservesDisconnectedGap() {
		MeetingTimeline first = MeetingTimeline.from(MEETING_STARTED_AT, MEETING_STARTED_AT);
		int firstStreamEnd = first.toAbsoluteMs(120_000);

		// 30초 끊겼다가 재연결
		MeetingTimeline second = MeetingTimeline.from(MEETING_STARTED_AT, MEETING_STARTED_AT.plusSeconds(150));
		int secondStreamStart = second.toAbsoluteMs(0);

		assertThat(secondStreamStart - firstStreamEnd).isEqualTo(30_000);
	}

	@Test
	@DisplayName("startedAt 이 없으면 0 오프셋으로 조용히 넘어가지 않고 예외로 막는다")
	void rejectsMissingStartedAt() {
		assertThatThrownBy(() -> MeetingTimeline.from(null, MEETING_STARTED_AT))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("서버 시계가 역행해도 음수 오프셋을 만들지 않는다")
	void clampsNegativeOffset() {
		MeetingTimeline timeline = MeetingTimeline.from(MEETING_STARTED_AT, MEETING_STARTED_AT.minusSeconds(10));

		assertThat(timeline.baseOffsetMs()).isZero();
	}

	@Test
	@DisplayName("INTEGER 범위를 넘으면 조용히 wrap 되지 않고 예외로 막는다")
	void rejectsTimestampBeyondIntegerRange() {
		MeetingTimeline timeline = new MeetingTimeline(Integer.MAX_VALUE);

		assertThatThrownBy(() -> timeline.toAbsoluteMs(1))
				.isInstanceOf(GeneralException.class);
	}

	@Test
	@DisplayName("음수 스트림 타임스탬프는 합산 결과가 유효해도 거부한다")
	void rejectsNegativeStreamRelativeMs() {
		MeetingTimeline timeline = new MeetingTimeline(1000L);

		// baseOffsetMs=1000, streamRelativeMs=-1 -> 합산은 999로 유효해 보이지만 입력 자체가 비정상이다.
		assertThatThrownBy(() -> timeline.toAbsoluteMs(-1))
				.isInstanceOf(GeneralException.class);
	}
}
