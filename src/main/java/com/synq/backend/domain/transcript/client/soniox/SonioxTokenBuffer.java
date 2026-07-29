package com.synq.backend.domain.transcript.client.soniox;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Soniox 토큰을 세그먼트 단위로 조립한다. 외부 의존이 없는 순수 로직이라 단위테스트로 검증한다.
 *
 * <p>확정 규칙
 * <ul>
 *   <li>is_final 토큰은 <b>누적</b>한다.</li>
 *   <li>interim 토큰은 매 프레임의 가설 전체이므로 누적이 아니라 <b>교체</b>한다. (누적하면 텍스트가 폭증한다)</li>
 *   <li>{@code <end>} 토큰을 만나면 그 시점까지의 누적분을 확정하고 버퍼를 비운다. — 주 경로</li>
 *   <li>{@code <end>} 가 오지 않아도 idleTimeout 동안 새 확정 토큰이 없으면 강제 확정한다. — 안전망</li>
 * </ul>
 *
 * <p>확정 경로가 두 개(Soniox 리더 스레드 / 유휴 flush 스케줄러 스레드)라 모든 상태 변경 메서드를
 * synchronized 로 직렬화한다. 두 스레드가 동시에 flush 해도 버퍼를 비운 쪽만 값을 받으므로
 * 세그먼트가 중복 저장되지 않는다.
 */
public class SonioxTokenBuffer {

	private final List<SonioxToken> finalTokens = new ArrayList<>();
	private String interimText = "";
	private Instant lastFinalTokenAt;

	/**
	 * 한 응답 프레임의 토큰들을 반영한다.
	 * 한 프레임에 {@code <end>} 가 여러 개 올 수 있어 확정 결과는 리스트로 돌려준다.
	 */
	public synchronized BufferUpdate accept(List<SonioxToken> tokens, Instant now) {
		List<FinalizedSegment> finalized = new ArrayList<>();
		StringBuilder interimOfThisFrame = new StringBuilder();

		for (SonioxToken token : tokens) {
			if (token.isEndMarker()) {
				// <end> 이전까지만 확정한다. 같은 프레임의 이후 토큰은 다음 세그먼트로 넘어간다.
				flush().ifPresent(finalized::add);
				continue;
			}
			if (token.isFinMarker()) {
				continue;
			}
			if (token.confirmed()) {
				finalTokens.add(token);
				lastFinalTokenAt = now;
				continue;
			}
			interimOfThisFrame.append(token.text() == null ? "" : token.text());
		}

		this.interimText = interimOfThisFrame.toString();
		return new BufferUpdate(finalized, caption());
	}

	/**
	 * {@code <end>} 가 오지 않는 경우의 안전망. 마지막 확정 토큰 이후 idleTimeout 이 지났으면 강제 확정한다.
	 * 시간을 인자로 받아 테스트에서 시계를 조작할 수 있게 한다.
	 */
	public synchronized Optional<FinalizedSegment> flushIfIdle(Instant now, Duration idleTimeout) {
		if (finalTokens.isEmpty() || lastFinalTokenAt == null) {
			return Optional.empty();
		}
		if (Duration.between(lastFinalTokenAt, now).compareTo(idleTimeout) < 0) {
			return Optional.empty();
		}
		return flush();
	}

	/** 회의 종료 시 남은 누적분을 마지막 세그먼트로 확정한다. 이게 없으면 종료 직전 발화가 유실된다. */
	public synchronized Optional<FinalizedSegment> flushRemaining() {
		return flush();
	}

	/** 확정분 + 현재 interim. 호스트 화면 캡션 push 용이며 저장하지 않는다. */
	public synchronized String caption() {
		StringBuilder builder = new StringBuilder();
		appendTokenTexts(builder, finalTokens);
		builder.append(interimText);
		return builder.toString().strip();
	}

	private Optional<FinalizedSegment> flush() {
		if (finalTokens.isEmpty()) {
			return Optional.empty();
		}

		StringBuilder builder = new StringBuilder();
		appendTokenTexts(builder, finalTokens);
		String content = builder.toString().strip();

		int startMs = finalTokens.get(0).startMsOrZero();
		int endMs = finalTokens.stream()
				.mapToInt(SonioxToken::endMsOrZero)
				.max()
				.orElse(startMs);

		finalTokens.clear();
		lastFinalTokenAt = null;

		// 공백/표식만 쌓였던 경우는 저장하지 않는다. (content 는 NOT NULL + 빈 문자열 금지)
		if (content.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(new FinalizedSegment(content, startMs, Math.max(startMs, endMs)));
	}

	private static void appendTokenTexts(StringBuilder builder, List<SonioxToken> tokens) {
		for (SonioxToken token : tokens) {
			if (token.isMarker() || token.text() == null) {
				continue;
			}
			builder.append(token.text());
		}
	}

	/** accept 한 번의 결과. 확정 세그먼트와 갱신된 캡션을 한 번에 돌려줘 찢어진 상태를 읽지 않게 한다. */
	public record BufferUpdate(List<FinalizedSegment> finalized, String caption) {
	}
}
