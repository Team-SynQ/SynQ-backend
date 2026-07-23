package com.synq.backend.domain.meeting.event;

/**
 * 회의가 종료(SUMMARIZING 전환)되었음을 다른 도메인에 알리는 계약이다.
 * ai.summary 도메인이 이를 수신해 AI 정리 생성을 시작한다.
 */
public record MeetingEndedEvent(Long meetingId) {
}
