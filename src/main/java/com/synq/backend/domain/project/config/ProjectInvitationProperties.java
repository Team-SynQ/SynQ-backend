package com.synq.backend.domain.project.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "project.invitation")
public record ProjectInvitationProperties(
		@NotBlank
		@Pattern(
				regexp = "https?://[^/?#]+/?",
				message = "프로젝트 초대 링크의 프론트엔드 기본 URL에는 경로를 포함할 수 없습니다."
		)
		String frontendBaseUrl,
		@Positive long expirationDays
) {
}
