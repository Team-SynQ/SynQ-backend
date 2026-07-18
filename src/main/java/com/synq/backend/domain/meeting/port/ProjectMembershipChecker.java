package com.synq.backend.domain.meeting.port;

/**
 * 구현(어댑터)은 project 도메인에서 제공
 */
public interface ProjectMembershipChecker {

	boolean isMember(Long projectId, Long userId);
}
