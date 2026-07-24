package com.synq.backend.domain.ai.event;

/**
 * AI 정리 생성이 실패했음을 알리는 계약이다.
 * meeting 도메인이 이를 수신해 회의 상태를 SUMMARY_FAILED 로 전이한다(재시도 API로 복구 가능).
 */
public record SummaryFailedEvent(Long meetingId, String reason) {
}
