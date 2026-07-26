package com.synq.backend.domain.ai.assistant.mock;

import com.synq.backend.domain.ai.assistant.port.MemberPerspective;
import com.synq.backend.domain.ai.assistant.port.ProjectMemberPerspectivePort;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Project 도메인에 역할·관점 컬럼이 생기기 전까지 쓰는 결정적 관점 대역.
 * AI 클라이언트(fake/openai) 토글과 무관하게, 실제 어댑터가 나오기 전까지 상시 활성이다.
 */
@Component
public class FakeProjectMemberPerspectivePort implements ProjectMemberPerspectivePort {

	@Override
	public Optional<MemberPerspective> find(Long projectId, Long userId) {
		return Optional.of(new MemberPerspective("백엔드 개발자", "일정보다 안정성을 우선한다."));
	}
}
