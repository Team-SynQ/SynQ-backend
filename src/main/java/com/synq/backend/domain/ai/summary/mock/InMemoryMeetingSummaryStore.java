package com.synq.backend.domain.ai.summary.mock;

import com.synq.backend.domain.ai.summary.domain.MeetingSummary;
import com.synq.backend.domain.ai.summary.domain.MeetingSummaryStore;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryMeetingSummaryStore implements MeetingSummaryStore {

	// #23 Mock 단계의 임시 저장소다. 실제 구현에서는 meeting_summary 테이블 어댑터로 교체한다.
	private final Map<Long, MeetingSummary> summaries = new ConcurrentHashMap<>();

	@Override
	public MeetingSummary save(MeetingSummary summary) {
		int nextVersion = findLatestByMeetingId(summary.meetingId())
				.map(value -> value.version() + 1)
				.orElse(1);
		MeetingSummary versioned = summary.withVersion(nextVersion);
		summaries.put(summary.meetingId(), versioned);
		return versioned;
	}

	@Override
	public Optional<MeetingSummary> findLatestByMeetingId(Long meetingId) {
		return Optional.ofNullable(summaries.get(meetingId));
	}
}
