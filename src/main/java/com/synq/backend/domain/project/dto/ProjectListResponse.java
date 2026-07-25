package com.synq.backend.domain.project.dto;

import com.synq.backend.domain.project.entity.Project;

import java.time.LocalDateTime;

public record ProjectListResponse(
		Long projectId,
		String title,
		String description,
		String recentMeetingTitle,
		LocalDateTime updatedAt
) {
	public static ProjectListResponse from(
			Project project,
			String recentMeetingTitle,
			LocalDateTime updatedAt
	) {
		return new ProjectListResponse(
				project.getId(),
				project.getTitle(),
				project.getDescription(),
				recentMeetingTitle,
				updatedAt
		);
	}
}
