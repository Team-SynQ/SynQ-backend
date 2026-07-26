package com.synq.backend.domain.auth.jwt;

import org.springframework.util.StringUtils;

import java.util.Optional;

public final class BearerTokenExtractor {

	private static final String BEARER_PREFIX = "Bearer ";

	private BearerTokenExtractor() {
	}

	public static Optional<String> extract(String authorizationHeader) {
		if (!StringUtils.hasText(authorizationHeader)
				|| authorizationHeader.length() <= BEARER_PREFIX.length()
				|| !authorizationHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
			return Optional.empty();
		}
		String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
		return StringUtils.hasText(token) ? Optional.of(token) : Optional.empty();
	}
}
