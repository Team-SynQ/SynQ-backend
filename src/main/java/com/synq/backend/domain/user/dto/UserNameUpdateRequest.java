package com.synq.backend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserNameUpdateRequest(
		@NotBlank @Size(max = 20) String name
) {
}
