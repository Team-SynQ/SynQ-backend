package com.synq.backend.domain.project.dto;

import com.synq.backend.domain.project.entity.ProjectParticipationRequest;

import java.time.OffsetDateTime;

public record ProjectJoinRequestCreateResponse(
		Long requestId,
		Long projectId,
		String status,
		OffsetDateTime requestedAt
) {
	public static ProjectJoinRequestCreateResponse from(ProjectParticipationRequest request) {
		return new ProjectJoinRequestCreateResponse(
				request.getId(),
				request.getProjectId(),
				request.getStatus().name(),
				request.getRequestedAt()
		);
	}
}
