package com.synq.backend.domain.meeting.repository;

import com.synq.backend.domain.meeting.entity.MeetingParticipant;
import com.synq.backend.domain.meeting.entity.ParticipantRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {

	List<MeetingParticipant> findByMeetingIdOrderByJoinedAtAscIdAsc(Long meetingId);

	List<MeetingParticipant> findByMeetingIdAndRole(Long meetingId, ParticipantRole role);

	List<MeetingParticipant> findByMeetingIdInAndRole(List<Long> meetingIds, ParticipantRole role);

	List<MeetingParticipant> findByMeetingIdAndUserId(Long meetingId, Long userId);

	boolean existsByMeetingIdAndUserId(Long meetingId, Long userId);

	Optional<MeetingParticipant> findByMeetingIdAndUserIdAndLeftAtIsNull(Long meetingId, Long userId);

	boolean existsByMeetingIdAndUserIdAndLeftAtIsNull(Long meetingId, Long userId);
}
