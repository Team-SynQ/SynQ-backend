package com.synq.backend.domain.ai.assistant.service;

import com.synq.backend.domain.ai.assistant.application.HintStore;
import com.synq.backend.domain.ai.assistant.domain.HintAiClient;
import com.synq.backend.domain.ai.assistant.domain.HintInput;
import com.synq.backend.domain.ai.assistant.domain.HintResult;
import com.synq.backend.domain.ai.assistant.domain.SegmentHint;
import com.synq.backend.domain.meeting.service.MeetingParticipantAccessValidator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HintService {

	private final HintContextBuilder contextBuilder;
	private final HintAiClient hintAiClient;
	private final MeetingParticipantAccessValidator accessValidator;
	private final HintStore hintStore;

	public HintResult generate(Long userId, Long meetingId, Long segmentId) {
		accessValidator.validateActiveParticipant(meetingId, userId);
		HintInput input = contextBuilder.build(userId, meetingId, segmentId);
		HintResult result = hintAiClient.generate(input);
		saveQuietly(meetingId, segmentId, userId, result);
		return result;
	}

	/**
	 * 회의 기록 화면이 읽는 내 힌트 목록. 회의가 끝난 뒤에도 조회된다
	 * (참여자 검증이 leftAt 만 보고 회의 상태는 보지 않는다).
	 */
	public List<SegmentHint> getMyHints(Long userId, Long meetingId) {
		accessValidator.validateActiveParticipant(meetingId, userId);
		return hintStore.findMyHints(meetingId, userId);
	}

	// 저장 실패로 응답을 깨지 않는다. 힌트는 이미 생성됐고, 회의 중 기능이라
	// 기록을 못 남긴 것보다 사용자 화면에 힌트가 안 뜨는 쪽이 손해가 크다.
	private void saveQuietly(Long meetingId, Long segmentId, Long userId, HintResult result) {
		try {
			hintStore.save(meetingId, segmentId, userId, result);
		} catch (RuntimeException e) {
			log.warn("3-hint 저장 실패 meetingId={} segmentId={} userId={}", meetingId, segmentId, userId, e);
		}
	}
}
