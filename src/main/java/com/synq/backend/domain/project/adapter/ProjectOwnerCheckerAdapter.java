package com.synq.backend.domain.project.adapter;

import com.synq.backend.domain.meeting.port.ProjectOwnerChecker;
import com.synq.backend.domain.project.repository.ProjectRepository;
import org.springframework.stereotype.Component;

/**
 * meeting 도메인의 ProjectOwnerChecker 포트를 project 도메인이 구현한 어댑터.
 * 소유자 판별은 project.owner_id 를 기준으로 한다.
 */
@Component
public class ProjectOwnerCheckerAdapter implements ProjectOwnerChecker {

	private final ProjectRepository projectRepository;

	public ProjectOwnerCheckerAdapter(ProjectRepository projectRepository) {
		this.projectRepository = projectRepository;
	}

	@Override
	public boolean isOwner(Long projectId, Long userId) {
		return projectRepository.findById(projectId)
				.map(project -> project.getOwnerId().equals(userId))
				.orElse(false);
	}
}
