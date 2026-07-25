package com.synq.backend.domain.ai.assistant.repository;

import com.synq.backend.domain.ai.assistant.domain.AiChatMessage;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {

	Optional<AiChatMessage> findByMeetingIdAndUserIdAndClientRequestId(
			Long meetingId,
			Long userId,
			UUID clientRequestId
	);

	Page<AiChatMessage> findByMeetingIdAndUserIdOrderByCreatedAtDescIdDesc(
			Long meetingId,
			Long userId,
			Pageable pageable
	);
}
