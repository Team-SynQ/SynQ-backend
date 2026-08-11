package com.synq.backend.domain.project.dto;

import com.synq.backend.domain.user.entity.Perspective;
import com.synq.backend.domain.user.entity.Role;

import java.util.List;

public record ProjectRolePerspectiveResponse(
		boolean useDefault,
		Role roleCategory,
		String detailRole,
		List<Perspective> perspectives
) {
	public static ProjectRolePerspectiveResponse from(
			boolean useDefault,
			Role roleCategory,
			String detailRole,
			List<Perspective> perspectives
	) {
		return new ProjectRolePerspectiveResponse(
				useDefault,
				roleCategory,
				detailRole,
				List.copyOf(perspectives)
		);
	}
}
