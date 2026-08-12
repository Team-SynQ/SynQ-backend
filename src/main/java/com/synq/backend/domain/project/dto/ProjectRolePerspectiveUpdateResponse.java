package com.synq.backend.domain.project.dto;

import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.user.entity.Perspective;
import com.synq.backend.domain.user.entity.Role;

import java.time.LocalDateTime;
import java.util.List;

public record ProjectRolePerspectiveUpdateResponse(
		Long projectId,
		boolean useDefault,
		Role roleCategory,
		String detailRole,
		List<Perspective> perspectives,
		LocalDateTime updatedAt
) {
	public static ProjectRolePerspectiveUpdateResponse from(
			ProjectMember member,
			List<Perspective> perspectives
	) {
		return new ProjectRolePerspectiveUpdateResponse(
				member.getProjectId(),
				member.isUseDefault(),
				member.getRoleCategory(),
				member.getDetailRole(),
				List.copyOf(perspectives),
				member.getUpdatedAt()
		);
	}
}
