package com.synq.backend.domain.project.dto;

import com.synq.backend.domain.project.entity.Project;

import java.time.LocalDateTime;

public record ProjectDetailResponse(
		Long projectId,
		Long ownerId,
		String title,
		String description,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
	public static ProjectDetailResponse from(Project project) {
		return new ProjectDetailResponse(
				project.getId(),
				project.getOwnerId(),
				project.getTitle(),
				project.getDescription(),
				project.getCreatedAt(),
				project.getUpdatedAt()
		);
	}
}
