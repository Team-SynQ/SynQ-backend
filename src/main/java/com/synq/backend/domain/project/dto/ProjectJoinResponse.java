package com.synq.backend.domain.project.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectMember;

import java.time.LocalDateTime;

public record ProjectJoinResponse(
		Long projectId,
		String title,
		String description,
		String memberRole,
		LocalDateTime joinedAt,
		@JsonIgnore boolean newlyJoined
) {
	public static ProjectJoinResponse from(Project project, ProjectMember member, boolean newlyJoined) {
		return new ProjectJoinResponse(
				project.getId(),
				project.getTitle(),
				project.getDescription(),
				member.getRole().name(),
				member.getJoinedAt(),
				newlyJoined
		);
	}
}
