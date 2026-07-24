package com.synq.backend.domain.ai.assistant.port;

import java.util.List;

/**
 * 클릭한 세그먼트와 앞뒤 윈도우. 세그먼트 한 개만으론
 * 맥락이 끊겨서, 앞뒤를 함께 담아 전달한다.
 */
public record TranscriptWindow(
		Long meetingId,
		int sequenceIndex,
		TranscriptSegmentView focus,
		List<TranscriptSegmentView> before,
		List<TranscriptSegmentView> after
) {
}
