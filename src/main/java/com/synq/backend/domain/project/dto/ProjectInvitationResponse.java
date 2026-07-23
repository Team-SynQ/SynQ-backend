package com.synq.backend.domain.project.dto;

import java.time.LocalDateTime;

public record ProjectInvitationResponse(
		String inviteUrl,
		LocalDateTime expiresAt
) {
}
