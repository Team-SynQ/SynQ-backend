package com.synq.backend.domain.ai.assistant.port;

import java.util.Optional;

/**
 * 프로젝트 멤버의 역할·관점. "내 영향" 힌트의 핵심 입력. 값은 프로젝트 단위다.
 */
public interface ProjectMemberPerspectivePort {

	Optional<MemberPerspective> find(Long projectId, Long userId);
}
