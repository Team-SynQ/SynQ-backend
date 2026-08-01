package com.synq.backend.domain.reference.dto;

import java.util.List;

public record ReferenceListResponse(
		Integer currentCount,
		Integer maxCount,
		List<ReferenceResponse> references
) {
	public static ReferenceListResponse from(int maxCount, List<ReferenceResponse> references) {
		return new ReferenceListResponse(
				references.size(),
				maxCount,
				references
		);
	}
}
