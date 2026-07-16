package com.synq.backend.domain.ai.summary.mock;

import com.synq.backend.domain.ai.summary.domain.MeetingSummary;
import com.synq.backend.domain.ai.summary.domain.MeetingSummaryStore;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryMeetingSummaryStore implements MeetingSummaryStore {

	// #23 Mock 단계의 임시 저장소다. 실제 구현에서는 meeting_summary 테이블 어댑터로 교체한다.
	private final Map<Long, MeetingSummary> summaries = new ConcurrentHashMap<>();

	@Override
	public MeetingSummary save(MeetingSummary summary) {
		summaries.put(summary.meetingId(), summary);
		return summary;
	}

	@Override
	public Optional<MeetingSummary> findLatestByMeetingId(Long meetingId) {
		return Optional.ofNullable(summaries.get(meetingId));
	}
}
