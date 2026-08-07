package com.synq.backend.domain.user.image;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "image.profile")
public record ProfileImageProperties(
		long maxSizeBytes,
		String cloudfrontDomain
) {
}
