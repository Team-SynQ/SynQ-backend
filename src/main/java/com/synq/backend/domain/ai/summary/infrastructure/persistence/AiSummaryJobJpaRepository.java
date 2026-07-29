package com.synq.backend.domain.ai.summary.infrastructure.persistence;

import com.synq.backend.domain.ai.summary.domain.SummaryJobStatus;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiSummaryJobJpaRepository extends JpaRepository<AiSummaryJobEntity, UUID> {

	Optional<AiSummaryJobEntity> findFirstByMeetingIdAndStatusInOrderByCreatedAtDesc(
			Long meetingId,
			Collection<SummaryJobStatus> statuses
	);
}
