package com.synq.backend.domain.reference.dto;

import java.util.List;

public record ReferenceFileCreateResponse(
		List<ReferenceFileResponse> references
) {
}
