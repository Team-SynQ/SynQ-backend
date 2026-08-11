package com.synq.backend.domain.ai.assistant.repository;

import com.synq.backend.domain.ai.assistant.domain.SegmentHint;
import com.synq.backend.domain.ai.assistant.domain.HintSource;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SegmentHintRepository extends JpaRepository<SegmentHint, Long> {

	Optional<SegmentHint> findByMeetingIdAndSegmentIdAndUserId(Long meetingId, Long segmentId, Long userId);

	boolean existsByMeetingIdAndSegmentIdAndUserId(Long meetingId, Long segmentId, Long userId);

	List<SegmentHint> findByMeetingIdAndUserIdOrderBySegmentIdAsc(Long meetingId, Long userId);

	List<SegmentHint> findTop5ByMeetingIdAndUserIdAndSourceOrderByCreatedAtDesc(
			Long meetingId, Long userId, HintSource source);
}
