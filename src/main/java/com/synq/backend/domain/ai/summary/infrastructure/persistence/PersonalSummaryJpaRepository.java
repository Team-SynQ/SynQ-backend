package com.synq.backend.domain.ai.summary.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalSummaryJpaRepository extends JpaRepository<PersonalSummaryEntity, Long> {

	Optional<PersonalSummaryEntity> findFirstByMeetingIdAndUserIdOrderByVersionDesc(Long meetingId, Long userId);
}
