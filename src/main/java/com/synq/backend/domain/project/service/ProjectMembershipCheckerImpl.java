package com.synq.backend.domain.project.service;

import com.synq.backend.domain.meeting.port.ProjectMembershipChecker;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!local & !test")
@RequiredArgsConstructor
public class ProjectMembershipCheckerImpl implements ProjectMembershipChecker {

	private final ProjectMemberRepository projectMemberRepository;

	@Override
	public boolean isMember(Long projectId, Long userId) {
		return projectMemberRepository.existsByProjectIdAndUserId(projectId, userId);
	}
}
