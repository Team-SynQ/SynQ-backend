package com.synq.backend.domain.project.dto;

import jakarta.validation.constraints.NotBlank;

public record ProjectJoinRequest(
		@NotBlank String inviteToken
) {
}
