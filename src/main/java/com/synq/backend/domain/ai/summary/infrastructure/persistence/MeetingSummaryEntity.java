package com.synq.backend.domain.ai.summary.infrastructure.persistence;

import com.synq.backend.domain.ai.summary.domain.GeneratedSummary;
import com.synq.backend.domain.ai.summary.domain.DiscussionSection;
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

	@Column(name = "one_line_summary", columnDefinition = "text")
	private String oneLineSummary;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "key_topics", nullable = false, columnDefinition = "jsonb")
	private List<String> keyTopics = new ArrayList<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "discussion_sections", columnDefinition = "jsonb")
	private List<DiscussionSection> discussionSections = new ArrayList<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private List<String> decisions = new ArrayList<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "action_items", nullable = false, columnDefinition = "jsonb")
	private List<String> actionItems = new ArrayList<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "open_questions", nullable = false, columnDefinition = "jsonb")
	private List<String> openQuestions = new ArrayList<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "tentative_directions", columnDefinition = "jsonb")
	private List<String> tentativeDirections = new ArrayList<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "confirmation_items", columnDefinition = "jsonb")
	private List<String> confirmationItems = new ArrayList<>();

	@Column(name = "generated_at", nullable = false)
	private Instant generatedAt;

	private MeetingSummaryEntity(MeetingSummary summary) {
		GeneratedSummary content = summary.content();
		this.meetingId = summary.meetingId();
		this.jobId = summary.jobId();
		this.version = summary.version();
		// 구버전 애플리케이션이 NOT NULL 기존 컬럼을 읽을 수 있도록 호환 값을 함께 저장한다.
		this.overallSummary = content.oneLineSummary();
		this.oneLineSummary = content.oneLineSummary();
		this.keyTopics = new ArrayList<>(content.keyTopics());
		this.discussionSections = new ArrayList<>(content.discussionSections());
		this.decisions = new ArrayList<>(content.decisions());
		// 새 필드가 없는 기존 행을 읽을 때 action_items를 confirmationItems로 되살릴 수 있게 매핑한다.
		this.actionItems = new ArrayList<>(content.confirmationItems());
		// 새 필드가 없는 기존 행을 읽을 때 open_questions를 tentativeDirections로 되살릴 수 있게 매핑한다.
		this.openQuestions = new ArrayList<>(content.tentativeDirections());
		this.tentativeDirections = new ArrayList<>(content.tentativeDirections());
		this.confirmationItems = new ArrayList<>(content.confirmationItems());
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
				new GeneratedSummary(
						oneLineSummary == null ? overallSummary : oneLineSummary,
						keyTopics,
						discussionSections,
						decisions,
						tentativeDirections == null ? openQuestions : tentativeDirections,
						confirmationItems == null ? actionItems : confirmationItems
				),
				generatedAt
		);
	}
}
