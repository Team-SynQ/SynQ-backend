package com.synq.backend.domain.user.dto;

import com.synq.backend.domain.user.entity.Perspective;
import com.synq.backend.domain.user.entity.Role;

import java.util.List;

public record RoleProfileResponse(
		Long id,
		boolean isDefault,
		Role role,
		String detailRole,
		List<Perspective> perspectives
) {
}
