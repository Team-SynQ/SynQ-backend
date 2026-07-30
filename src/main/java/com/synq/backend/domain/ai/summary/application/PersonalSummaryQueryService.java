package com.synq.backend.domain.ai.summary.application;

import com.synq.backend.domain.ai.summary.domain.PersonalSummary;
import com.synq.backend.domain.ai.summary.domain.PersonalSummaryStore;
import com.synq.backend.global.apipayload.code.GeneralErrorCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import org.springframework.stereotype.Service;

@Service
public class PersonalSummaryQueryService {

	private final PersonalSummaryStore personalSummaryStore;

	public PersonalSummaryQueryService(PersonalSummaryStore personalSummaryStore) {
		this.personalSummaryStore = personalSummaryStore;
	}

	public PersonalSummary getLatest(Long meetingId, Long userId) {
		return personalSummaryStore.findLatestByMeetingIdAndUserId(meetingId, userId)
				.orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND));
	}
}
