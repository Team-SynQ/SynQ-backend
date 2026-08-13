package com.synq.backend.domain.transcript.repository;

import com.synq.backend.domain.transcript.entity.MeetingRecordingSegment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetingRecordingSegmentRepository extends JpaRepository<MeetingRecordingSegment, Long> {

	// id 순서가 곧 세그먼트 생성 순서이자 재생 순서다.
	List<MeetingRecordingSegment> findByMeetingIdOrderByIdAsc(Long meetingId);
}
