package com.synq.backend.domain.ai.summary.domain;

import java.util.List;

public interface PersonalSummaryTargetReader {

	List<PersonalSummaryTarget> findByMeetingId(Long meetingId);
}
