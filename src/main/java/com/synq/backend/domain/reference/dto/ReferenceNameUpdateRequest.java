package com.synq.backend.domain.reference.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReferenceNameUpdateRequest(
		@NotBlank @Size(max = 30) String name
) {
	public ReferenceNameUpdateRequest {
		if (name != null) {
			name = name.trim();
		}
	}
}
