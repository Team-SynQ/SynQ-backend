package com.synq.backend.domain.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectCreateRequest(
		@NotBlank @Size(max = 30) String title,
		@Size(max = 500) String description
) {
}
