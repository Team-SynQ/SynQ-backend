package com.synq.backend.domain.ai.assistant.mock;

import com.synq.backend.domain.ai.assistant.port.MemberPerspective;
import com.synq.backend.domain.ai.assistant.port.ProjectMemberPerspectivePort;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "ai.assistant", name = "client", havingValue = "fake")
public class FakeProjectMemberPerspectivePort implements ProjectMemberPerspectivePort {

	@Override
	public Optional<MemberPerspective> find(Long projectId, Long userId) {
		return Optional.of(new MemberPerspective("백엔드 개발자", "일정보다 안정성을 우선한다."));
	}
}
