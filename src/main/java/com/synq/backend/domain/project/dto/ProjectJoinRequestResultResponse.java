package com.synq.backend.domain.project.dto;

import java.time.OffsetDateTime;

public record ProjectJoinRequestResultResponse(
		Long requestId,
		Long projectId,
		String projectTitle,
		String status,
		OffsetDateTime decidedAt
) {
}
