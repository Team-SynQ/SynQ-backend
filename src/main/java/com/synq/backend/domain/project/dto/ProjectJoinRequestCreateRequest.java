package com.synq.backend.domain.project.dto;

import com.synq.backend.domain.project.entity.ProjectJoinSettingSource;
import com.synq.backend.domain.user.entity.Perspective;
import com.synq.backend.domain.user.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProjectJoinRequestCreateRequest(
		@NotBlank String inviteToken,
		@NotNull ProjectJoinSettingSource settingSource,
		@NotNull Role roleCategory,
		@Size(max = 30) String detailRole,
		@NotNull @Size(max = 3) List<@NotNull Perspective> perspectives
) {
}
