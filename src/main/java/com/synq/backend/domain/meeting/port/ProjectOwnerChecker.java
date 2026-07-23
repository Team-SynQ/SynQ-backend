package com.synq.backend.domain.meeting.port;

/**
 * 회의 종료 등 소유자 전용 동작의 권한 판별용 포트.
 * 구현(어댑터)은 project 도메인에서 제공한다.
 */
public interface ProjectOwnerChecker {

	boolean isOwner(Long projectId, Long userId);
}
