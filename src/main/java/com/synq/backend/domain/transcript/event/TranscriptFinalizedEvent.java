package com.synq.backend.domain.transcript.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * STT가 확정한 전사 세그먼트를 다른 도메인에 전달하는 계약이다.
 * 중간 전사(interim)는 저장하거나 AI에 전달하지 않고, 확정 전사만 이 이벤트를 발행한다.
 */
public record TranscriptFinalizedEvent(
		@NotNull Long meetingId,
		@NotNull Long segmentId,
		@NotNull Integer sequenceIndex,
		@NotNull Integer startMs,
		@NotNull Integer endMs,
		@NotBlank String content,
		String speakerLabel
) {
}
