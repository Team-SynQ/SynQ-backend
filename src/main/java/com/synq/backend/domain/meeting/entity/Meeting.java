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

// 호스트는 별도 FK가 아니라 MeetingParticipant.role=HOST 로 판별한다 (멀티 참여자 모델 지원).
@Entity
@Table(name = "meeting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Meeting extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "project_id", nullable = false)
	private Long projectId;

	@Column(nullable = false)
	private String title;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MeetingStatus status;

	@Column(name = "started_at")
	private LocalDateTime startedAt;

	@Column(name = "ended_at")
	private LocalDateTime endedAt;

	private Meeting(Long projectId, String title, MeetingStatus status) {
		this.projectId = projectId;
		this.title = title;
		this.status = status;
	}

	public static Meeting of(Long projectId, String title) {
		return new Meeting(projectId, title, MeetingStatus.IN_PROGRESS);
	}
}
