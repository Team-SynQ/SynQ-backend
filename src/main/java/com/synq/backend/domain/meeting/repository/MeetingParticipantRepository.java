package com.synq.backend.domain.meeting.repository;

import com.synq.backend.domain.meeting.entity.MeetingParticipant;
import com.synq.backend.domain.meeting.entity.ParticipantRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {

	List<MeetingParticipant> findByMeetingIdAndRole(Long meetingId, ParticipantRole role);

	List<MeetingParticipant> findByMeetingIdAndUserId(Long meetingId, Long userId);
}
