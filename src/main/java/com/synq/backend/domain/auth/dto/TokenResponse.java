package com.synq.backend.domain.auth.dto;

public record TokenResponse(
		String accessToken,
		String refreshToken,
		boolean isNewUser
) {
}
