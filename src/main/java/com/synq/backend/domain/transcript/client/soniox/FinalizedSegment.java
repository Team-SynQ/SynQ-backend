package com.synq.backend.domain.transcript.client.soniox;

/**
 * 확정된 세그먼트 하나. startMs/endMs 는 아직 Soniox 스트림 기준(연결마다 0 부터 시작)이며,
 * 회의 절대 시각으로의 변환은 MeetingTimeline 이 담당한다.
 */
public record FinalizedSegment(String content, int startMs, int endMs) {
}
