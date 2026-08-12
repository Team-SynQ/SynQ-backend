package com.synq.backend.domain.project.dto;

import java.util.List;

public record ProjectJoinRequestListResponse(
		Integer pendingCount,
		List<ProjectJoinRequestResponse> requests
) {
	public static ProjectJoinRequestListResponse from(List<ProjectJoinRequestResponse> requests) {
		return new ProjectJoinRequestListResponse(requests.size(), requests);
	}
}
