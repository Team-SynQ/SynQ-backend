package com.synq.backend.domain.project.dto;

import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.user.entity.User;

import java.time.LocalDateTime;

public record ProjectMemberResponse(
		Long memberId,
		Long userId,
		String nickname,
		String role,
		Boolean isMe,
		LocalDateTime joinedAt
) {
	public static ProjectMemberResponse from(ProjectMember member, User user, Long currentUserId) {
		return new ProjectMemberResponse(
				member.getId(),
				member.getUserId(),
				user.getName(),
				member.getRole().name(),
				member.getUserId().equals(currentUserId),
				member.getJoinedAt()
		);
	}
}
