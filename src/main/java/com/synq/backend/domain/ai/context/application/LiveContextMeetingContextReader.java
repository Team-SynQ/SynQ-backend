package com.synq.backend.domain.ai.context.application;

import com.synq.backend.domain.ai.context.repository.LiveContextRepository;
import com.synq.backend.domain.ai.summary.domain.MeetingContextReader;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 회의 후 요약이 저장된 Live Context의 누적 요약을 재사용하도록 연결하는 어댑터다.
 */
@Component
@ConditionalOnProperty(prefix = "ai.summary", name = "context-source", havingValue = "live-context")
public class LiveContextMeetingContextReader implements MeetingContextReader {

	private final LiveContextRepository liveContextRepository;

	public LiveContextMeetingContextReader(LiveContextRepository liveContextRepository) {
		this.liveContextRepository = liveContextRepository;
	}

	@Override
	public Optional<String> findRollingSummary(Long meetingId) {
		return liveContextRepository.findByMeetingId(meetingId)
				.map(context -> context.getRollingSummary());
	}
}
