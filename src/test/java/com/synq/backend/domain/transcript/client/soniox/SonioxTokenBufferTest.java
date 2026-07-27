package com.synq.backend.domain.transcript.client.soniox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SonioxTokenBufferTest {

	private static final Instant T0 = Instant.parse("2026-07-27T10:00:00Z");
	private static final Duration IDLE_TIMEOUT = Duration.ofSeconds(5);

	private final SonioxTokenBuffer buffer = new SonioxTokenBuffer();

	private static SonioxToken finalToken(String text, int startMs, int endMs) {
		return new SonioxToken(text, startMs, endMs, true, "ko");
	}

	private static SonioxToken interimToken(String text) {
		return new SonioxToken(text, null, null, false, "ko");
	}

	private static SonioxToken endMarker() {
		return new SonioxToken(SonioxToken.END_MARKER, null, null, true, null);
	}

	@Test
	@DisplayName("<end> 를 만나면 누적된 확정 토큰이 한 세그먼트로 확정된다")
	void finalizesOnEndMarker() {
		buffer.accept(List.of(finalToken("안녕", 0, 500), finalToken("하세요", 500, 1000)), T0);

		SonioxTokenBuffer.BufferUpdate update = buffer.accept(List.of(endMarker()), T0);

		assertThat(update.finalized()).hasSize(1);
		FinalizedSegment segment = update.finalized().get(0);
		assertThat(segment.content()).isEqualTo("안녕하세요");
		assertThat(segment.startMs()).isZero();
		assertThat(segment.endMs()).isEqualTo(1000);
	}

	@Test
	@DisplayName("확정 후 버퍼가 비워져 다음 세그먼트에 이전 내용이 섞이지 않는다")
	void clearsBufferAfterFinalize() {
		buffer.accept(List.of(finalToken("첫 번째", 0, 500), endMarker()), T0);

		SonioxTokenBuffer.BufferUpdate update = buffer.accept(List.of(finalToken("두 번째", 1000, 1500), endMarker()), T0);

		assertThat(update.finalized()).hasSize(1);
		assertThat(update.finalized().get(0).content()).isEqualTo("두 번째");
	}

	@Test
	@DisplayName("interim 토큰은 누적이 아니라 교체된다")
	void replacesInterimInsteadOfAccumulating() {
		buffer.accept(List.of(interimToken("안녕")), T0);
		buffer.accept(List.of(interimToken("안녕하")), T0);
		SonioxTokenBuffer.BufferUpdate update = buffer.accept(List.of(interimToken("안녕하세요")), T0);

		// 누적됐다면 "안녕안녕하안녕하세요" 가 된다.
		assertThat(update.caption()).isEqualTo("안녕하세요");
	}

	@Test
	@DisplayName("interim 은 저장 대상이 아니므로 <end> 가 와도 세그먼트를 만들지 않는다")
	void doesNotFinalizeInterimOnlyBuffer() {
		buffer.accept(List.of(interimToken("아직 확정 안 됨")), T0);

		SonioxTokenBuffer.BufferUpdate update = buffer.accept(List.of(endMarker()), T0);

		assertThat(update.finalized()).isEmpty();
	}

	@Test
	@DisplayName("빈 버퍼에서 <end> 가 와도 빈 세그먼트를 만들지 않는다")
	void ignoresEndMarkerOnEmptyBuffer() {
		SonioxTokenBuffer.BufferUpdate update = buffer.accept(List.of(endMarker()), T0);

		assertThat(update.finalized()).isEmpty();
	}

	@Test
	@DisplayName("한 프레임에 <end> 가 두 번 오면 세그먼트도 두 개로 나뉜다")
	void splitsOnMultipleEndMarkersInOneFrame() {
		SonioxTokenBuffer.BufferUpdate update = buffer.accept(List.of(
				finalToken("첫 문장", 0, 500),
				endMarker(),
				finalToken("둘째 문장", 600, 1200),
				endMarker()
		), T0);

		assertThat(update.finalized()).hasSize(2);
		assertThat(update.finalized().get(0).content()).isEqualTo("첫 문장");
		assertThat(update.finalized().get(1).content()).isEqualTo("둘째 문장");
		assertThat(update.finalized().get(1).startMs()).isEqualTo(600);
	}

	@Test
	@DisplayName("표식 토큰은 전사 내용에 포함되지 않는다")
	void excludesMarkerTokensFromContent() {
		SonioxTokenBuffer.BufferUpdate update = buffer.accept(List.of(
				finalToken("내용", 0, 500),
				new SonioxToken(SonioxToken.FIN_MARKER, null, null, true, null),
				endMarker()
		), T0);

		assertThat(update.finalized().get(0).content()).isEqualTo("내용");
	}

	@Test
	@DisplayName("토큰 선행 공백이 유지되고 세그먼트 양끝만 다듬어진다")
	void keepsInnerSpacingAndTrimsEdges() {
		SonioxTokenBuffer.BufferUpdate update = buffer.accept(List.of(
				finalToken(" 이 ", 0, 300),
				finalToken("API ", 300, 600),
				finalToken("스펙", 600, 900),
				endMarker()
		), T0);

		assertThat(update.finalized().get(0).content()).isEqualTo("이 API 스펙");
	}

	@Test
	@DisplayName("공백만 누적된 경우 저장하지 않는다")
	void ignoresWhitespaceOnlyBuffer() {
		SonioxTokenBuffer.BufferUpdate update = buffer.accept(List.of(finalToken("   ", 0, 100), endMarker()), T0);

		assertThat(update.finalized()).isEmpty();
	}

	@Test
	@DisplayName("<end> 가 오지 않아도 유휴 시간이 지나면 강제로 확정된다")
	void forceFlushesAfterIdleTimeout() {
		buffer.accept(List.of(finalToken("끝나지 않는 발화", 0, 1000)), T0);

		Optional<FinalizedSegment> flushed = buffer.flushIfIdle(T0.plusSeconds(6), IDLE_TIMEOUT);

		assertThat(flushed).isPresent();
		assertThat(flushed.get().content()).isEqualTo("끝나지 않는 발화");
	}

	@Test
	@DisplayName("유휴 시간이 지나지 않았으면 강제 확정하지 않는다")
	void doesNotFlushBeforeIdleTimeout() {
		buffer.accept(List.of(finalToken("말하는 중", 0, 1000)), T0);

		assertThat(buffer.flushIfIdle(T0.plusSeconds(3), IDLE_TIMEOUT)).isEmpty();
	}

	@Test
	@DisplayName("확정 토큰이 계속 들어오면 유휴 타이머가 갱신되어 중간에 끊기지 않는다")
	void resetsIdleTimerOnNewFinalToken() {
		buffer.accept(List.of(finalToken("앞부분", 0, 1000)), T0);
		buffer.accept(List.of(finalToken(" 뒷부분", 1000, 2000)), T0.plusSeconds(4));

		assertThat(buffer.flushIfIdle(T0.plusSeconds(6), IDLE_TIMEOUT)).isEmpty();
		assertThat(buffer.flushIfIdle(T0.plusSeconds(10), IDLE_TIMEOUT)).isPresent();
	}

	@Test
	@DisplayName("강제 확정 후 버퍼가 비워져 같은 내용이 두 번 저장되지 않는다")
	void doesNotFlushTwiceAfterIdleFlush() {
		buffer.accept(List.of(finalToken("한 번만", 0, 1000)), T0);

		assertThat(buffer.flushIfIdle(T0.plusSeconds(6), IDLE_TIMEOUT)).isPresent();
		assertThat(buffer.flushIfIdle(T0.plusSeconds(20), IDLE_TIMEOUT)).isEmpty();
	}

	@Test
	@DisplayName("회의 종료 시 남은 누적분이 마지막 세그먼트로 확정된다")
	void flushesRemainingOnClose() {
		buffer.accept(List.of(finalToken("종료 직전 발화", 5000, 6000)), T0);

		Optional<FinalizedSegment> flushed = buffer.flushRemaining();

		assertThat(flushed).isPresent();
		assertThat(flushed.get().content()).isEqualTo("종료 직전 발화");
		assertThat(buffer.flushRemaining()).isEmpty();
	}

	@Test
	@DisplayName("캡션은 확정분과 interim 을 이어붙인 현재 상태를 돌려준다")
	void captionCombinesFinalAndInterim() {
		buffer.accept(List.of(finalToken("확정된 부분", 0, 1000)), T0);

		SonioxTokenBuffer.BufferUpdate update = buffer.accept(List.of(interimToken(" 말하는 중")), T0);

		assertThat(update.caption()).isEqualTo("확정된 부분 말하는 중");
	}

	@Test
	@DisplayName("start_ms/end_ms 가 없는 토큰이 와도 세그먼트를 만든다")
	void handlesTokensWithoutTimestamps() {
		SonioxTokenBuffer.BufferUpdate update = buffer.accept(List.of(
				new SonioxToken("타임스탬프 없음", null, null, true, "ko"),
				endMarker()
		), T0);

		assertThat(update.finalized()).hasSize(1);
		assertThat(update.finalized().get(0).startMs()).isZero();
		assertThat(update.finalized().get(0).endMs()).isZero();
	}
}
