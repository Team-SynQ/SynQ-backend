package com.synq.backend.domain.ai.summary.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingSummaryJpaRepository extends JpaRepository<MeetingSummaryEntity, Long> {

	Optional<MeetingSummaryEntity> findFirstByMeetingIdOrderByVersionDesc(Long meetingId);
}
