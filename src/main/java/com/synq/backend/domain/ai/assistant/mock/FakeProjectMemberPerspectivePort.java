package com.synq.backend.domain.ai.assistant.mock;

import com.synq.backend.domain.ai.assistant.port.MemberPerspective;
import com.synq.backend.domain.ai.assistant.port.ProjectMemberPerspectivePort;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "test"})
public class FakeProjectMemberPerspectivePort implements ProjectMemberPerspectivePort {

	@Override
	public Optional<MemberPerspective> find(Long projectId, Long userId) {
		return Optional.of(new MemberPerspective("백엔드 개발자", "일정보다 안정성을 우선한다."));
	}
}
