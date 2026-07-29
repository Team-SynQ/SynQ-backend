package com.synq.backend.domain.ai.summary.mock;

import com.synq.backend.domain.ai.summary.domain.PersonalSummary;
import com.synq.backend.domain.ai.summary.domain.PersonalSummaryStore;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryPersonalSummaryStore implements PersonalSummaryStore {

	private final Map<String, PersonalSummary> summaries = new ConcurrentHashMap<>();

	@Override
	public void saveAll(List<PersonalSummary> values) {
		values.forEach(summary -> summaries.put(key(summary.meetingId(), summary.userId()), summary));
	}

	@Override
	public Optional<PersonalSummary> findLatestByMeetingIdAndUserId(Long meetingId, Long userId) {
		return Optional.ofNullable(summaries.get(key(meetingId, userId)));
	}

	private String key(Long meetingId, Long userId) {
		return meetingId + ":" + userId;
	}
}
