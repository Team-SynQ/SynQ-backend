package com.synq.backend.domain.project.dto;

import com.synq.backend.domain.project.entity.Project;

import java.time.LocalDateTime;

public record ProjectUpdateResponse(
		Long projectId,
		String title,
		String description,
		LocalDateTime updatedAt
) {
	public static ProjectUpdateResponse from(Project project) {
		return new ProjectUpdateResponse(
				project.getId(),
				project.getTitle(),
				project.getDescription(),
				project.getUpdatedAt()
		);
	}
}
