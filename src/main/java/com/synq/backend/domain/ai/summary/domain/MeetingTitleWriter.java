package com.synq.backend.domain.ai.summary.domain;

public interface MeetingTitleWriter {

	void updateTitle(Long meetingId, String title);
}
