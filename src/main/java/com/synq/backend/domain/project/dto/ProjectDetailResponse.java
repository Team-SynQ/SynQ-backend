package com.synq.backend.domain.project.dto;

import com.synq.backend.domain.project.entity.Project;

import java.time.LocalDateTime;

public record ProjectDetailResponse(
		Long projectId,
		Long ownerId,
		String title,
		String description,
		Long activeMeetingId,
		LocalDateTime activeMeetingStartedAt,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
	public static ProjectDetailResponse from(Project project, Long activeMeetingId, LocalDateTime activeMeetingStartedAt) {
		return new ProjectDetailResponse(
				project.getId(),
				project.getOwnerId(),
				project.getTitle(),
				project.getDescription(),
				activeMeetingId,
				activeMeetingStartedAt,
				project.getCreatedAt(),
				project.getUpdatedAt()
		);
	}
}
