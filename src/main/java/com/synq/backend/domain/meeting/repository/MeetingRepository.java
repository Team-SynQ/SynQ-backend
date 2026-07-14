package com.synq.backend.domain.meeting.repository;

import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.entity.MeetingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

	boolean existsByProjectIdAndStatus(Long projectId, MeetingStatus status);
}
