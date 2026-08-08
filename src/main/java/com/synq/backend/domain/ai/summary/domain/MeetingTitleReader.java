package com.synq.backend.domain.ai.summary.domain;

import java.util.Optional;

/**
 * 전체 요약 응답에 표시할 회의 제목을 조회하는 포트.
 */
public interface MeetingTitleReader {

	Optional<String> findTitle(Long meetingId);
}
