package com.synq.backend.domain.meeting.repository;

import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.entity.MeetingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

	boolean existsByProjectIdAndStatus(Long projectId, MeetingStatus status);

	@Query("""
			SELECT meeting
			FROM Meeting meeting
			WHERE meeting.projectId IN :projectIds
			  AND meeting.startedAt = (
				  SELECT MAX(candidate.startedAt)
				  FROM Meeting candidate
				  WHERE candidate.projectId = meeting.projectId
			  )
			""")
	List<Meeting> findRecentMeetingsByProjectIds(@Param("projectIds") List<Long> projectIds);
}
