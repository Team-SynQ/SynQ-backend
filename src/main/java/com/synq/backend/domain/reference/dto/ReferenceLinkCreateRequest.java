package com.synq.backend.domain.reference.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.net.URI;

public record ReferenceLinkCreateRequest(
		@NotBlank @Size(max = 2000) String url
) {
	public ReferenceLinkCreateRequest {
		if (url != null) {
			url = url.trim();
		}
	}

	@AssertTrue(message = "http 또는 https 형식의 URL이어야 합니다.")
	@JsonIgnore
	public boolean isValidUrl() {
		if (url == null || url.isBlank()) {
			return true;
		}
		try {
			URI uri = URI.create(url);
			String scheme = uri.getScheme();
			String host = uri.getHost();
			String displayName = host != null && host.regionMatches(true, 0, "www.", 0, 4)
					? host.substring(4)
					: host;
			return scheme != null
					&& (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
					&& displayName != null
					&& !displayName.isBlank()
					&& displayName.length() <= 255;
		} catch (IllegalArgumentException exception) {
			return false;
		}
	}
}
