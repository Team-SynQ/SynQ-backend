package com.synq.backend.domain.project.dto;

import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.user.entity.Role;
import com.synq.backend.domain.user.entity.User;

public record ProjectInvitationOwnerResponse(
		Long userId,
		String name,
		String profileImageUrl,
		Role roleCategory
) {
	public static ProjectInvitationOwnerResponse from(
			ProjectMember owner,
			User user,
			String profileImageUrl,
			Role roleCategory
	) {
		return new ProjectInvitationOwnerResponse(
				owner.getUserId(),
				user.getName(),
				profileImageUrl,
				roleCategory
		);
	}
}
