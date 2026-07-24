package com.synq.backend.domain.ai.assistant.port;

import java.util.Optional;

/**
 * 회의 → 프로젝트 매핑. meeting.project_id 는 이미 존재한다.
 * 실구현은 MeetingRepository 를 백킹으로 두면 된다.
 */
public interface MeetingProjectPort {

	Optional<Long> findProjectId(Long meetingId);
}
