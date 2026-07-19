package com.synq.backend.domain.ai.summary.domain;

/**
 * 전사 도메인에서 시간순으로 전달받는 확정 발화다.
 * speakerLabel은 STT 제공자가 화자 라벨을 제공한 경우에만 채워지며, MVP 요약에는 사용하지 않는다.
 */
public record TranscriptSegment(String speakerLabel, String content) {
}
