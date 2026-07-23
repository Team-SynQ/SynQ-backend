package com.synq.backend.domain.project.dto;

import com.synq.backend.domain.project.entity.Project;

import java.time.LocalDateTime;

public record ProjectInvitationInfoResponse(
		Long projectId,
		String title,
		String description,
		Integer currentMemberCount,
		Integer maxMemberCount,
		Boolean alreadyJoined,
		LocalDateTime expiresAt
) {
	public static ProjectInvitationInfoResponse from(
			Project project,
			int currentMemberCount,
			int maxMemberCount,
			boolean alreadyJoined
	) {
		return new ProjectInvitationInfoResponse(
				project.getId(),
				project.getTitle(),
				project.getDescription(),
				currentMemberCount,
				maxMemberCount,
				alreadyJoined,
				project.getInviteTokenExpiresAt()
		);
	}
}
