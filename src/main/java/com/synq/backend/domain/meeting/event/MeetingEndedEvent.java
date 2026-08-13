package com.synq.backend.domain.meeting.event;

import java.util.UUID;

/**
 * 회의가 종료(SUMMARIZING 전환)되었음을 다른 도메인에 알리는 계약이다.
 * ai.summary 도메인이 이를 수신해 AI 정리 생성을 시작한다.
 */
public record MeetingEndedEvent(Long meetingId, UUID summaryJobId) {

	/**
	 * 요약 Job을 만들지 않는 기존 종료 이벤트 발행부와의 호환을 위한 생성자.
	 */
	public MeetingEndedEvent(Long meetingId) {
		this(meetingId, null);
	}
}
