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

	private Meeting(Long projectId, String title, MeetingStatus status, LocalDateTime startedAt) {
		this.projectId = projectId;
		this.title = title;
		this.status = status;
		this.startedAt = startedAt;
	}

	// 별도 "녹음 시작" API 없이 생성과 동시에 녹음이 시작된다.
	public static Meeting of(Long projectId, String title) {
		return new Meeting(projectId, title, MeetingStatus.IN_PROGRESS, LocalDateTime.now());
	}

	// 종료 호출 시 즉시 SUMMARIZING 으로 전환하고 종료 시각을 기록한다.
	// 이후 AI 정리 결과에 따라 markSummarized/markSummaryFailed 로 상태가 확정된다.
	public void end() {
		this.status = MeetingStatus.SUMMARIZING;
		this.endedAt = LocalDateTime.now();
	}

	public void markSummarized() {
		this.status = MeetingStatus.SUMMARIZED;
	}

	public void markSummaryFailed() {
		this.status = MeetingStatus.SUMMARY_FAILED;
	}
}
