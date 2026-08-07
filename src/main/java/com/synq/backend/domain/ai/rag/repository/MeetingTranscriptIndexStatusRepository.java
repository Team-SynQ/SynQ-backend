package com.synq.backend.domain.ai.rag.repository;

import com.synq.backend.domain.ai.rag.entity.MeetingTranscriptIndexStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MeetingTranscriptIndexStatusRepository
		extends JpaRepository<MeetingTranscriptIndexStatus, Long> {

	Optional<MeetingTranscriptIndexStatus> findByMeetingId(Long meetingId);
}
