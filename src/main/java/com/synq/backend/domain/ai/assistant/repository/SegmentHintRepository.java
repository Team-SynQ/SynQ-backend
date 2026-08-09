package com.synq.backend.domain.ai.assistant.repository;

import com.synq.backend.domain.ai.assistant.domain.SegmentHint;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SegmentHintRepository extends JpaRepository<SegmentHint, Long> {

	Optional<SegmentHint> findByMeetingIdAndSegmentIdAndUserId(Long meetingId, Long segmentId, Long userId);

	List<SegmentHint> findByMeetingIdAndUserIdOrderBySegmentIdAsc(Long meetingId, Long userId);
}
