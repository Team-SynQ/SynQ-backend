package com.synq.backend.domain.user.dto;

import com.synq.backend.domain.user.entity.Perspective;
import com.synq.backend.domain.user.entity.Role;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RoleProfileRequest(
		@NotNull Role role,
		@Size(max = 30) String detailRole,
		@Size(max = 3) List<Perspective> perspectives
) {
}
