package com.synq.backend.domain.ai.assistant.service;

import com.synq.backend.domain.ai.assistant.domain.HintAiClient;
import com.synq.backend.domain.ai.assistant.domain.HintInput;
import com.synq.backend.domain.ai.assistant.domain.HintResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HintService {

	private final HintContextBuilder contextBuilder;
	private final HintAiClient hintAiClient;

	public HintResult generate(Long userId, Long meetingId, Long segmentId) {
		HintInput input = contextBuilder.build(userId, meetingId, segmentId);
		return hintAiClient.generate(input);
	}
}
