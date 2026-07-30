package com.synq.backend.domain.ai.summary.infrastructure.persistence;

import com.synq.backend.domain.ai.summary.domain.SummaryJob;
import com.synq.backend.domain.ai.summary.domain.SummaryJobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_summary_job")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiSummaryJobEntity {

	@Id
	private UUID id;

	@Column(name = "meeting_id", nullable = false)
	private Long meetingId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SummaryJobStatus status;

	@Column(name = "retry_count", nullable = false)
	private int retryCount;

	@Column(name = "model_name", length = 100)
	private String modelName;

	@Column(name = "prompt_version", nullable = false, length = 50)
	private String promptVersion;

	@Column(name = "error_message", columnDefinition = "text")
	private String errorMessage;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "started_at")
	private Instant startedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	private AiSummaryJobEntity(SummaryJob job) {
		apply(job);
	}

	public static AiSummaryJobEntity from(SummaryJob job) {
		return new AiSummaryJobEntity(job);
	}

	public void apply(SummaryJob job) {
		this.id = job.id();
		this.meetingId = job.meetingId();
		this.status = job.status();
		this.retryCount = job.retryCount();
		this.modelName = job.modelName();
		this.promptVersion = job.promptVersion();
		this.errorMessage = job.errorMessage();
		this.createdAt = job.createdAt();
		this.startedAt = job.startedAt();
		this.completedAt = job.completedAt();
	}

	public SummaryJob toDomain() {
		return new SummaryJob(
				id,
				meetingId,
				status,
				retryCount,
				modelName,
				promptVersion,
				errorMessage,
				createdAt,
				startedAt,
				completedAt
		);
	}
}
