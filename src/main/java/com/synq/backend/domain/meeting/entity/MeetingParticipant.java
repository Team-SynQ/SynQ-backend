package com.synq.backend.domain.meeting.entity;

import com.synq.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "meeting_participant")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingParticipant extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "meeting_id", nullable = false)
	private Long meetingId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private ParticipantRole role;

	@Column(name = "joined_at", nullable = false)
	private LocalDateTime joinedAt;

	@Column(name = "left_at")
	private LocalDateTime leftAt;

	private MeetingParticipant(Long meetingId, Long userId, ParticipantRole role, LocalDateTime joinedAt) {
		this.meetingId = meetingId;
		this.userId = userId;
		this.role = role;
		this.joinedAt = joinedAt;
	}

	public static MeetingParticipant of(Long meetingId, Long userId, ParticipantRole role) {
		return new MeetingParticipant(meetingId, userId, role, LocalDateTime.now());
	}
}
