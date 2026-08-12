package com.synq.backend.domain.project.dto;

import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.project.entity.ProjectParticipationRequest;

import java.time.LocalDateTime;

public record ProjectJoinRequestApproveResponse(
		Long requestId,
		Long memberId,
		Long userId,
		String status,
		LocalDateTime joinedAt
) {
	public static ProjectJoinRequestApproveResponse from(
			ProjectParticipationRequest request,
			ProjectMember member
	) {
		return new ProjectJoinRequestApproveResponse(
				request.getId(),
				member.getId(),
				member.getUserId(),
				request.getStatus().name(),
				member.getJoinedAt()
		);
	}
}
