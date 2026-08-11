package com.synq.backend.domain.ai.event;

import com.synq.backend.domain.ai.assistant.domain.SegmentHint;

/**
 * 참여자 개인에게만 전송해야 하는 자동 3-hint 생성 완료 이벤트다.
 */
public record AutoHintCreatedEvent(
		Long meetingId,
		Long userId,
		SegmentHint hint
) {
}
