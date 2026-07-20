package com.synq.backend.domain.ai.context.repository;

import com.synq.backend.domain.ai.context.domain.LiveContext;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LiveContextRepository extends JpaRepository<LiveContext, Long> {

	Optional<LiveContext> findByMeetingId(Long meetingId);
}
