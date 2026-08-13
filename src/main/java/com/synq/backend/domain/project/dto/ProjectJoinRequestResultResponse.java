package com.synq.backend.domain.project.dto;

import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectParticipationRequest;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public record ProjectJoinRequestResultResponse(
		Long requestId,
		Long projectId,
		String projectTitle,
		String status,
		OffsetDateTime decidedAt
) {
	private static final ZoneId STORED_TIMESTAMP_ZONE = ZoneId.of("Asia/Seoul");

	public static ProjectJoinRequestResultResponse from(
			ProjectParticipationRequest request,
			Project project
	) {
		return new ProjectJoinRequestResultResponse(
				request.getId(),
				request.getProjectId(),
				project.getTitle(),
				request.getStatus().name(),
				request.getUpdatedAt()
						.atZone(STORED_TIMESTAMP_ZONE)
						.withZoneSameInstant(ZoneOffset.UTC)
						.toOffsetDateTime()
		);
	}
}
