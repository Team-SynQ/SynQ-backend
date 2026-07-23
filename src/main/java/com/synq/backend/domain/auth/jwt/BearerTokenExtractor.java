package com.synq.backend.domain.auth.jwt;

import org.springframework.util.StringUtils;

import java.util.Optional;

public final class BearerTokenExtractor {

	private static final String BEARER_PREFIX = "Bearer ";

	private BearerTokenExtractor() {
	}

	public static Optional<String> extract(String authorizationHeader) {
		if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			return Optional.empty();
		}
		return Optional.of(authorizationHeader.substring(BEARER_PREFIX.length()));
	}
}
