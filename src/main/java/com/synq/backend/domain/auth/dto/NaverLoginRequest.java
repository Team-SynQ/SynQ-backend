package com.synq.backend.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record NaverLoginRequest(
		@NotBlank String code,
		@NotBlank String state
) {
}
