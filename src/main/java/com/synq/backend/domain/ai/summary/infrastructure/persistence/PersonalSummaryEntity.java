package com.synq.backend.domain.ai.summary.infrastructure.persistence;

import com.synq.backend.domain.ai.summary.domain.GeneratedPersonalSummary;
import com.synq.backend.domain.ai.summary.domain.PersonalSummary;
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
@Table(name = "personal_summary")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersonalSummaryEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "meeting_id", nullable = false)
	private Long meetingId;

	@Column(name = "job_id", nullable = false)
	private UUID jobId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(length = 100)
	private String role;

	@Column(nullable = false)
	private int version;

	@Column(name = "personal_summary", nullable = false, columnDefinition = "text")
	private String personalSummary;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "key_points", nullable = false, columnDefinition = "jsonb")
	private List<String> keyPoints = new ArrayList<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "my_action_items", nullable = false, columnDefinition = "jsonb")
	private List<String> myActionItems = new ArrayList<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "follow_up_questions", nullable = false, columnDefinition = "jsonb")
	private List<String> followUpQuestions = new ArrayList<>();

	@Column(name = "generated_at", nullable = false)
	private Instant generatedAt;

	private PersonalSummaryEntity(PersonalSummary summary) {
		GeneratedPersonalSummary content = summary.content();
		this.meetingId = summary.meetingId();
		this.jobId = summary.jobId();
		this.userId = summary.userId();
		this.role = summary.role();
		this.version = summary.version();
		this.personalSummary = content.personalSummary();
		this.keyPoints = new ArrayList<>(content.keyPoints());
		this.myActionItems = new ArrayList<>(content.myActionItems());
		this.followUpQuestions = new ArrayList<>(content.followUpQuestions());
		this.generatedAt = summary.generatedAt();
	}

	public static PersonalSummaryEntity from(PersonalSummary summary) {
		return new PersonalSummaryEntity(summary);
	}

	public PersonalSummary toDomain() {
		return new PersonalSummary(
				meetingId,
				jobId,
				userId,
				role,
				version,
				new GeneratedPersonalSummary(personalSummary, keyPoints, myActionItems, followUpQuestions),
				generatedAt
		);
	}
}
