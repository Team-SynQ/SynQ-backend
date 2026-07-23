package com.synq.backend.domain.ai.context.domain;

import com.synq.backend.domain.transcript.event.TranscriptFinalizedEvent;
import com.synq.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 회의별 최신 AI 맥락을 보관한다. 전체 전사는 전사 도메인이 보관하고,
 * 이 엔티티는 다음 AI 호출에 필요한 압축된 상태만 유지한다.
 */
@Entity
@Table(name = "meeting_live_context")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LiveContext extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "meeting_id", nullable = false, unique = true)
	private Long meetingId;

	@Column(name = "rolling_summary", nullable = false, columnDefinition = "text")
	private String rollingSummary;

	@Column(name = "current_topic", columnDefinition = "text")
	private String currentTopic;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private List<String> decisions = new ArrayList<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "action_items", nullable = false, columnDefinition = "jsonb")
	private List<String> actionItems = new ArrayList<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "open_questions", nullable = false, columnDefinition = "jsonb")
	private List<String> openQuestions = new ArrayList<>();

	@Column(name = "last_segment_id", nullable = false)
	private Long lastSegmentId;

	@Column(name = "last_sequence_index", nullable = false)
	private Integer lastSequenceIndex;

	@Version
	@Column(nullable = false)
	private Long version;

	private LiveContext(Long meetingId, LiveContextResult result, TranscriptFinalizedEvent event) {
		this.meetingId = meetingId;
		apply(result, event);
	}

	public static LiveContext create(Long meetingId, LiveContextResult result, TranscriptFinalizedEvent event) {
		return new LiveContext(meetingId, result, event);
	}

	public boolean hasProcessed(TranscriptFinalizedEvent event) {
		return event.sequenceIndex() <= lastSequenceIndex;
	}

	public void update(LiveContextResult result, TranscriptFinalizedEvent event) {
		apply(result, event);
	}

	private void apply(LiveContextResult result, TranscriptFinalizedEvent event) {
		this.rollingSummary = result.rollingSummary();
		this.currentTopic = result.currentTopic();
		this.decisions = new ArrayList<>(result.decisions());
		this.actionItems = new ArrayList<>(result.actionItems());
		this.openQuestions = new ArrayList<>(result.openQuestions());
		this.lastSegmentId = event.segmentId();
		this.lastSequenceIndex = event.sequenceIndex();
	}
}
