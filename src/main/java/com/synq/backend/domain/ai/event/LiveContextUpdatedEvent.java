package com.synq.backend.domain.ai.event;

import com.synq.backend.domain.ai.context.domain.LiveContextSnapshot;

/**
 * 확정 전사가 Live Context에 반영된 뒤, 화면 갱신을 알리는 내부 이벤트다.
 */
public record LiveContextUpdatedEvent(
		Long meetingId,
		LiveContextSnapshot context,
		Long lastSegmentId,
		Integer lastSequenceIndex
) {
}
