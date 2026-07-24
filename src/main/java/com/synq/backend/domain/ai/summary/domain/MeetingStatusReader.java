package com.synq.backend.domain.ai.summary.domain;

/**
 * 요약 생성 가능 여부 판별을 위해 meeting 도메인의 상태를 조회하는 포트.
 * 구현(어댑터)은 meeting 도메인에서 제공한다.
 */
public interface MeetingStatusReader {

	/** 회의가 존재하고 종료(IN_PROGRESS 가 아님)되어 요약을 시작할 수 있는 상태인지. */
	boolean isEnded(Long meetingId);
}
