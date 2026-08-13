package com.synq.backend.domain.meeting.event;

/**
 * 진행자가 회의를 재개했음을 다른 도메인에 알리는 계약이다.
 * transcript 도메인이 이를 수신해 참여자에게 WS로 브로드캐스트한다.
 */
public record MeetingResumedEvent(Long meetingId, long activeSeconds) {
}
