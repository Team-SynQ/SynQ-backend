package com.synq.backend.domain.ai.assistant.service;

import com.synq.backend.domain.ai.assistant.code.AssistantErrorCode;
import com.synq.backend.domain.ai.assistant.domain.HintAiClient;
import com.synq.backend.domain.ai.assistant.domain.HintInput;
import com.synq.backend.domain.ai.assistant.domain.HintResult;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.global.apipayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HintService {

	private final HintContextBuilder contextBuilder;
	private final HintAiClient hintAiClient;
	private final MeetingParticipantRepository meetingParticipantRepository;

	public HintResult generate(Long userId, Long meetingId, Long segmentId) {
		// 맥락 조립보다 먼저 검증한다.
		// 회의 종료 여부는 보지 않는다 — 3-hint 는 결과를 저장하지 않아 종료 후 클릭을 막을 실익이 없다.
		if (!meetingParticipantRepository.existsByMeetingIdAndUserIdAndLeftAtIsNull(meetingId, userId)) {
			throw new GeneralException(AssistantErrorCode.NOT_MEETING_PARTICIPANT);
		}
		HintInput input = contextBuilder.build(userId, meetingId, segmentId);
		return hintAiClient.generate(input);
	}
}
