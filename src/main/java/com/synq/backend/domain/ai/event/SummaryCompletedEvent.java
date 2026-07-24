package com.synq.backend.domain.ai.event;

/**
 * AI 정리 생성이 완료되었음을 알리는 계약이다.
 * meeting 도메인이 이를 수신해 회의 상태를 SUMMARIZED 로 전이한다.
 */
public record SummaryCompletedEvent(Long meetingId) {
}
