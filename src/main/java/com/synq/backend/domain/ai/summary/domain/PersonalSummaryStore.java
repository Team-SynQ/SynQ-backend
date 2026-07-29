package com.synq.backend.domain.ai.summary.domain;

import java.util.List;
import java.util.Optional;

public interface PersonalSummaryStore {

	void saveAll(List<PersonalSummary> summaries);

	Optional<PersonalSummary> findLatestByMeetingIdAndUserId(Long meetingId, Long userId);
}
