package com.synq.backend.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.synq.backend.domain.user.entity.User;

public record UserMeResponse(
		Long userId,
		String name,
		@JsonInclude(JsonInclude.Include.ALWAYS) String email,
		String provider,
		@JsonInclude(JsonInclude.Include.ALWAYS) String profileImageUrl
) {
	public static UserMeResponse from(User user, String profileImageUrl) {
		return new UserMeResponse(user.getUserId(), user.getName(), user.getEmail(), user.getProvider().name(), profileImageUrl);
	}
}
