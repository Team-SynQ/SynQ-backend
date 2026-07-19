package com.synq.backend.domain.ai.summary.domain;

import com.synq.backend.domain.ai.summary.domain.MeetingSummary;
import java.util.Optional;

public interface MeetingSummaryStore {
	MeetingSummary save(MeetingSummary summary);

	Optional<MeetingSummary> findLatestByMeetingId(Long meetingId);
}
