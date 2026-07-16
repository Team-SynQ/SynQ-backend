package com.synq.backend.domain.ai.summary.domain;

import com.synq.backend.domain.ai.summary.domain.TranscriptSegment;
import java.util.List;

public interface TranscriptReader {
	List<TranscriptSegment> findByMeetingId(Long meetingId);
}
