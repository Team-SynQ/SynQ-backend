package com.synq.backend.domain.ai.assistant.dto;

import com.synq.backend.domain.ai.assistant.domain.SegmentHint;
import java.util.List;

public record HintRecordListResponse(
		Long meetingId,
		List<HintRecordResponse> hints
) {
	public static HintRecordListResponse from(Long meetingId, List<SegmentHint> hints) {
		return new HintRecordListResponse(
				meetingId,
				hints.stream().map(HintRecordResponse::from).toList()
		);
	}
}
