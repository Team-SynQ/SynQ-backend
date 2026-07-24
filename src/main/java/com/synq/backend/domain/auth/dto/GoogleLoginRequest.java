package com.synq.backend.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
		@NotBlank String code
) {
}
