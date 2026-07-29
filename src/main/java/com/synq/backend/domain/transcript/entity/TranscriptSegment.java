package com.synq.backend.domain.transcript.entity;

import com.synq.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * STT 가 확정한 전사 세그먼트. 중간 전사(interim)는 저장하지 않는다.
 * startMs/endMs 는 meeting.startedAt 을 0 점으로 하는 회의 절대 시각이다.
 */
@Entity
@Table(name = "transcript_segment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TranscriptSegment extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "meeting_id", nullable = false)
	private Long meetingId;

	@Column(name = "sequence_index", nullable = false)
	private Integer sequenceIndex;

	@Column(name = "start_ms", nullable = false)
	private Integer startMs;

	@Column(name = "end_ms", nullable = false)
	private Integer endMs;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	// 화자분리 MVP 미구현이라 항상 null 로 저장한다. placeholder 치환은 AI 어댑터 매핑에서만 한다.
	@Column(name = "speaker_label", length = 50)
	private String speakerLabel;

	private TranscriptSegment(Long meetingId, Integer sequenceIndex, Integer startMs, Integer endMs,
							String content, String speakerLabel) {
		this.meetingId = meetingId;
		this.sequenceIndex = sequenceIndex;
		this.startMs = startMs;
		this.endMs = endMs;
		this.content = content;
		this.speakerLabel = speakerLabel;
	}

	public static TranscriptSegment of(Long meetingId, int sequenceIndex, int startMs, int endMs, String content) {
		return new TranscriptSegment(meetingId, sequenceIndex, startMs, endMs, content, null);
	}
}
