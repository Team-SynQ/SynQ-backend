package com.synq.backend.domain.ai.assistant.mock;

import com.synq.backend.domain.ai.assistant.port.MeetingProjectPort;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "test"})
public class FakeMeetingProjectPort implements MeetingProjectPort {

	@Override
	public Optional<Long> findProjectId(Long meetingId) {
		return Optional.of(1L);
	}
}
