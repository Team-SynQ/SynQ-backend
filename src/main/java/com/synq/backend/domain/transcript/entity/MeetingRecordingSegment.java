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
 * 회의 녹음 세그먼트 하나(호스트 WS 세션 하나, webm 스트림 하나)가 S3에 업로드된 기록.
 * 재연결이 없으면 회의당 한 건, 있으면 여러 건이 되고 id 순서가 곧 재생 순서다.
 */
@Entity
@Table(name = "meeting_recording_segment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingRecordingSegment extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "meeting_id", nullable = false)
	private Long meetingId;

	@Column(name = "storage_key", nullable = false)
	private String storageKey;

	private MeetingRecordingSegment(Long meetingId, String storageKey) {
		this.meetingId = meetingId;
		this.storageKey = storageKey;
	}

	public static MeetingRecordingSegment of(Long meetingId, String storageKey) {
		return new MeetingRecordingSegment(meetingId, storageKey);
	}
}
