package com.synq.backend.domain.user.dto;

import com.synq.backend.domain.user.entity.User;

public record UserMeResponse(
		Long userId,
		String name,
		String email,
		String provider
) {
	public static UserMeResponse from(User user) {
		return new UserMeResponse(user.getUserId(), user.getName(), user.getEmail(), user.getProvider().name());
	}
}
