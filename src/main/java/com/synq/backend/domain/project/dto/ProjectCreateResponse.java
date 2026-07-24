package com.synq.backend.domain.project.dto;

import com.synq.backend.domain.project.entity.Project;

import java.time.LocalDateTime;

public record ProjectCreateResponse(
		Long projectId,
		Long ownerId,
		String title,
		String description,
		LocalDateTime createdAt
) {
	public static ProjectCreateResponse from(Project project) {
		return new ProjectCreateResponse(
				project.getId(),
				project.getOwnerId(),
				project.getTitle(),
				project.getDescription(),
				project.getCreatedAt()
		);
	}
}
