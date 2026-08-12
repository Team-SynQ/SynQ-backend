package com.synq.backend.domain.project.dto;

import com.synq.backend.domain.project.entity.ProjectParticipationRequest;

public record ProjectJoinRequestRejectResponse(
		Long requestId,
		String status
) {
	public static ProjectJoinRequestRejectResponse from(ProjectParticipationRequest request) {
		return new ProjectJoinRequestRejectResponse(
				request.getId(),
				request.getStatus().name()
		);
	}
}
