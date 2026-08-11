package com.synq.backend.domain.ai.assistant.dto;

import com.synq.backend.domain.ai.assistant.domain.SegmentHint;
import com.synq.backend.domain.ai.assistant.domain.HintSource;
import java.time.LocalDateTime;

public record HintRecordResponse(
		Long segmentId,
		String meaning,
		String myImpact,
		String teamQuestion,
		HintSource source,
		Integer importance,
		String triggerReason,
		LocalDateTime generatedAt
) {
	public static HintRecordResponse from(SegmentHint hint) {
		return new HintRecordResponse(
				hint.getSegmentId(),
				hint.getMeaning(),
				hint.getMyImpact(),
				hint.getTeamQuestion(),
				hint.getSource(),
				hint.getImportance(),
				hint.getTriggerReason(),
				// 덮어쓰기 방식이라 updatedAt 이 곧 마지막 생성 시각이다.
				hint.getUpdatedAt()
		);
	}
}
