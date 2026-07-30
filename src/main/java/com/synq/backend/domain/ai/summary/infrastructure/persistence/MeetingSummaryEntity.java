package com.synq.backend.domain.ai.summary.infrastructure.persistence;

import com.synq.backend.domain.ai.summary.domain.GeneratedSummary;
import com.synq.backend.domain.ai.summary.domain.MeetingSummary;
import com.synq.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "meeting_summary")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingSummaryEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "meeting_id", nullable = false)
	private Long meetingId;

	@Column(name = "job_id", nullable = false)
	private UUID jobId;

	@Column(nullable = false)
	private int version;

	@Column(name = "overall_summary", nullable = false, columnDefinition = "text")
	private String overallSummary;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "key_topics", nullable = false, columnDefinition = "jsonb")
	private List<String> keyTopics = new ArrayList<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private List<String> decisions = new ArrayList<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "action_items", nullable = false, columnDefinition = "jsonb")
	private List<String> actionItems = new ArrayList<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "open_questions", nullable = false, columnDefinition = "jsonb")
	private List<String> openQuestions = new ArrayList<>();

	@Column(name = "generated_at", nullable = false)
	private Instant generatedAt;

	private MeetingSummaryEntity(MeetingSummary summary) {
		GeneratedSummary content = summary.content();
		this.meetingId = summary.meetingId();
		this.jobId = summary.jobId();
		this.version = summary.version();
		this.overallSummary = content.overallSummary();
		this.keyTopics = new ArrayList<>(content.keyTopics());
		this.decisions = new ArrayList<>(content.decisions());
		this.actionItems = new ArrayList<>(content.actionItems());
		this.openQuestions = new ArrayList<>(content.openQuestions());
		this.generatedAt = summary.generatedAt();
	}

	public static MeetingSummaryEntity from(MeetingSummary summary) {
		return new MeetingSummaryEntity(summary);
	}

	public MeetingSummary toDomain() {
		return new MeetingSummary(
				meetingId,
				jobId,
				version,
				new GeneratedSummary(overallSummary, keyTopics, decisions, actionItems, openQuestions),
				generatedAt
		);
	}
}
