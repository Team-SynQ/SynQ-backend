package com.synq.backend.domain.ai.rag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 회의 전사 인덱싱의 진행/결과.
 */
@Entity
@Table(name = "meeting_transcript_index_status")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingTranscriptIndexStatus {

	/** 사유가 길어져도 행이 비대해지지 않게 자른다. 스택트레이스가 통째로 들어오는 경우가 있다. */
	private static final int MAX_REASON_LENGTH = 1000;

	// 회의당 한 행이므로 meeting_id 가 곧 PK 다. 대리키를 두면 중복 행을 막을 수 없다.
	@Id
	@Column(name = "meeting_id")
	private Long meetingId;

	@Column(name = "project_id", nullable = false)
	private Long projectId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TranscriptIndexStatus status;

	@Column(name = "failure_reason", columnDefinition = "text")
	private String failureReason;

	@Column(name = "chunk_count", nullable = false)
	private int chunkCount;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	private MeetingTranscriptIndexStatus(Long meetingId, Long projectId) {
		this.meetingId = meetingId;
		this.projectId = projectId;
		this.status = TranscriptIndexStatus.PROCESSING;
		this.chunkCount = 0;
		this.updatedAt = OffsetDateTime.now();
	}

	public static MeetingTranscriptIndexStatus startProcessing(Long meetingId, Long projectId) {
		return new MeetingTranscriptIndexStatus(meetingId, projectId);
	}

	/** 재인덱싱 진입. 이전 실패 사유를 남겨두면 성공한 뒤에도 사유가 붙어 있어 오해를 부른다. */
	public void markProcessing() {
		this.status = TranscriptIndexStatus.PROCESSING;
		this.failureReason = null;
		this.chunkCount = 0;
		touch();
	}

	public void markCompleted(int chunkCount) {
		this.status = TranscriptIndexStatus.COMPLETED;
		this.failureReason = null;
		this.chunkCount = chunkCount;
		touch();
	}

	public void markFailed(String reason) {
		this.status = TranscriptIndexStatus.FAILED;
		this.failureReason = truncate(reason);
		// 실패 시 청크를 전부 지우므로 개수도 0 이어야 실제 저장량과 맞는다.
		this.chunkCount = 0;
		touch();
	}

	public void markSkipped() {
		this.status = TranscriptIndexStatus.SKIPPED;
		this.failureReason = null;
		this.chunkCount = 0;
		touch();
	}

	private void touch() {
		this.updatedAt = OffsetDateTime.now();
	}

	private static String truncate(String reason) {
		if (reason == null) {
			return null;
		}
		return reason.length() <= MAX_REASON_LENGTH ? reason : reason.substring(0, MAX_REASON_LENGTH);
	}
}
