package com.synq.backend.domain.project.dto;

import com.synq.backend.domain.project.entity.ProjectParticipationRequest;
import com.synq.backend.domain.user.entity.User;

import java.time.OffsetDateTime;

public record ProjectJoinRequestResponse(
		Long requestId,
		Long userId,
		String name,
		OffsetDateTime requestedAt
) {
	public static ProjectJoinRequestResponse from(ProjectParticipationRequest request, User user) {
		return new ProjectJoinRequestResponse(
				request.getId(),
				request.getUserId(),
				user.getName(),
				request.getRequestedAt()
		);
	}
}
