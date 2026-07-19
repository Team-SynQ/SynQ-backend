package com.synq.backend.domain.ai.summary.domain;

import java.util.Optional;

public interface MeetingContextReader {
	Optional<String> findRollingSummary(Long meetingId);
}
