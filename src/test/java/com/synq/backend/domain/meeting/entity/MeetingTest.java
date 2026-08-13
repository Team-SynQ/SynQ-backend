package com.synq.backend.domain.meeting.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MeetingTest {

	@Test
	void 생성_직후에는_일시정지_상태가_아니고_활성_시간이_0에_가깝다() {
		Meeting meeting = Meeting.of(1L, "회의");

		assertThat(meeting.isPaused()).isFalse();
		assertThat(meeting.activeSeconds()).isEqualTo(0L);
	}

	@Test
	void 일시정지하면_그_시점의_활성_시간에_고정된다() {
		Meeting meeting = Meeting.of(1L, "회의");

		meeting.pause();
		long pausedActiveSeconds = meeting.activeSeconds();

		assertThat(meeting.isPaused()).isTrue();
		// 일시정지 중에는 시간이 더 지나도 activeSeconds()가 그대로여야 한다.
		assertThat(meeting.activeSeconds()).isEqualTo(pausedActiveSeconds);
	}

	@Test
	void 재개하면_일시정지_상태가_풀리고_그_시점의_누적값부터_다시_흐른다() {
		Meeting meeting = Meeting.of(1L, "회의");

		meeting.pause();
		long pausedActiveSeconds = meeting.activeSeconds();
		meeting.resume();

		assertThat(meeting.isPaused()).isFalse();
		// 재개 직후에는 일시정지 시점의 누적값 이상이어야 한다(추가로 흐른 시간이 0에 가까움).
		assertThat(meeting.activeSeconds()).isGreaterThanOrEqualTo(pausedActiveSeconds);
	}
}
