package com.synq.backend.domain.transcript.dto;

import jakarta.validation.constraints.NotBlank;

public record TranscriptSegmentUpdateRequest(
		@NotBlank String content
) {
}
