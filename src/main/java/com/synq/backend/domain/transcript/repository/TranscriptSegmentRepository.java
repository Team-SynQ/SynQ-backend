package com.synq.backend.domain.transcript.repository;

import com.synq.backend.domain.transcript.entity.TranscriptSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface TranscriptSegmentRepository extends JpaRepository<TranscriptSegment, Long> {

	// 발화 순서 보장: 같은 start_ms 가 겹칠 수 있어 sequenceIndex 를 2차 정렬로 둔다.
	List<TranscriptSegment> findByMeetingIdOrderByStartMsAscSequenceIndexAsc(Long meetingId);

	// 폴링 클라이언트가 이미 받은 구간을 다시 받지 않도록, afterSequenceIndex 이후분만 조회한다.
	List<TranscriptSegment> findByMeetingIdAndSequenceIndexGreaterThanOrderByStartMsAscSequenceIndexAsc(
			Long meetingId, Integer afterSequenceIndex);

	List<TranscriptSegment> findByMeetingIdOrderByStartMsDescSequenceIndexDesc(Long meetingId, Pageable pageable);

	List<TranscriptSegment> findByMeetingIdAndSequenceIndexBetweenOrderByStartMsAscSequenceIndexAsc(
			Long meetingId,
			Integer fromSequenceIndex,
			Integer toSequenceIndex
	);
	// 경로의 meetingId 소속이 맞는지 조회 시점에 함께 확인한다.
	Optional<TranscriptSegment> findByIdAndMeetingId(Long id, Long meetingId);

	@Query("select max(s.sequenceIndex) from TranscriptSegment s where s.meetingId = :meetingId")
	Optional<Integer> findMaxSequenceByMeetingId(@Param("meetingId") Long meetingId);
}
