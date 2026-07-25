package com.synq.backend.domain.ai.assistant.mock;

import com.synq.backend.domain.ai.assistant.port.MeetingProjectPort;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "ai.assistant", name = "client", havingValue = "fake")
public class FakeMeetingProjectPort implements MeetingProjectPort {

	@Override
	public Optional<Long> findProjectId(Long meetingId) {
		return Optional.of(1L);
	}
}
