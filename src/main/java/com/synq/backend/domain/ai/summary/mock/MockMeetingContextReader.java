package com.synq.backend.domain.ai.summary.mock;

import com.synq.backend.domain.ai.summary.domain.MeetingContextReader;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "test"})
public class MockMeetingContextReader implements MeetingContextReader {

	@Override
	public Optional<String> findRollingSummary(Long meetingId) {
		return Optional.of("회의 후 AI 요약의 기본 흐름과 담당 범위를 논의 중입니다.");
	}
}
