package com.synq.backend.domain.meeting.mock;

import com.synq.backend.domain.meeting.port.ProjectMembershipChecker;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * PROJECT 도메인의 실제 멤버십 조회가 준비되기 전까지, 항상 멤버로 취급하는 대역이다.
 * 실제 ProjectMembershipChecker 구현체가 준비되면 이 대역은 제거한다.
 */
@Component
@Profile({"local", "test"})
public class AlwaysMemberProjectMembershipChecker implements ProjectMembershipChecker {

	@Override
	public boolean isMember(Long projectId, Long userId) {
		return true;
	}
}
