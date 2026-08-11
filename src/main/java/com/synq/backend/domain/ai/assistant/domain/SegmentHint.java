package com.synq.backend.domain.ai.assistant.domain;

import com.synq.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자가 세그먼트를 클릭해 생성한 3-hint 를 회의 종료 후에도 볼 수 있게 보관한다.
 *
 * <p>myImpact 가 사용자의 역할·관점에서 나오므로 행의 단위가 (meeting, segment, user) 다.
 * 같은 사용자가 다시 클릭하면 새 행을 쌓지 않고 {@link #overwrite} 로 덮어쓴다.
 */
@Entity
@Table(name = "ai_segment_hint")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SegmentHint extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "meeting_id", nullable = false)
	private Long meetingId;

	@Column(name = "segment_id", nullable = false)
	private Long segmentId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private HintSource source;

	@Column
	private Integer importance;

	@Column(name = "trigger_reason", columnDefinition = "text")
	private String triggerReason;

	@Column(name = "topic", columnDefinition = "text")
	private String topic;

	@Column(nullable = false, columnDefinition = "text")
	private String meaning;

	@Column(name = "my_impact", nullable = false, columnDefinition = "text")
	private String myImpact;

	@Column(name = "team_question", nullable = false, columnDefinition = "text")
	private String teamQuestion;

	private SegmentHint(
			Long meetingId,
			Long segmentId,
			Long userId,
			HintResult result,
			HintSource source,
			Integer importance,
			String triggerReason,
			String topic
	) {
		this.meetingId = meetingId;
		this.segmentId = segmentId;
		this.userId = userId;
		this.source = source;
		this.importance = importance;
		this.triggerReason = triggerReason;
		this.topic = topic;
		this.meaning = result.meaning();
		this.myImpact = result.myImpact();
		this.teamQuestion = result.teamQuestion();
	}

	public static SegmentHint of(Long meetingId, Long segmentId, Long userId, HintResult result) {
		return new SegmentHint(meetingId, segmentId, userId, result, HintSource.MANUAL, null, null, null);
	}

	public static SegmentHint autoOf(
			Long meetingId,
			Long segmentId,
			Long userId,
			HintResult result,
			int importance,
			String triggerReason,
			String topic
	) {
		return new SegmentHint(meetingId, segmentId, userId, result, HintSource.AUTO, importance, triggerReason, topic);
	}

	public static SegmentHint autoOf(
			Long meetingId,
			Long segmentId,
			Long userId,
			HintResult result,
			int importance,
			String triggerReason
	) {
		return autoOf(meetingId, segmentId, userId, result, importance, triggerReason, null);
	}

	public void overwrite(HintResult result) {
		this.source = HintSource.MANUAL;
		this.importance = null;
		this.triggerReason = null;
		this.topic = null;
		this.meaning = result.meaning();
		this.myImpact = result.myImpact();
		this.teamQuestion = result.teamQuestion();
	}
}
