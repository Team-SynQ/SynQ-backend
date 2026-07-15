package com.synq.backend.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
		@NotBlank @Size(max = 20) String name,
		@NotBlank @Email String email,
		@NotBlank @Size(max = 72) String password
) {
}
