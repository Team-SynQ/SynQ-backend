package com.synq.backend.domain.project.dto;

import com.synq.backend.domain.project.entity.Project;

import java.util.List;

public record ProjectMemberListResponse(
		Long projectId,
		Long ownerId,
		String title,
		Integer currentMemberCount,
		Integer maxMemberCount,
		List<ProjectMemberResponse> members
) {
	public static ProjectMemberListResponse from(
			Project project,
			int maxMemberCount,
			List<ProjectMemberResponse> members
	) {
		return new ProjectMemberListResponse(
				project.getId(),
				project.getOwnerId(),
				project.getTitle(),
				members.size(),
				maxMemberCount,
				members
		);
	}
}
