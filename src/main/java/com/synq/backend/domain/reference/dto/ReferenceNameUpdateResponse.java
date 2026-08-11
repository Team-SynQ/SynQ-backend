package com.synq.backend.domain.reference.dto;

import com.synq.backend.domain.reference.entity.ReferenceMaterial;

import java.time.LocalDateTime;

public record ReferenceNameUpdateResponse(
		Long referenceId,
		String name,
		String type,
		LocalDateTime updatedAt
) {
	public static ReferenceNameUpdateResponse from(ReferenceMaterial reference) {
		return new ReferenceNameUpdateResponse(
				reference.getId(),
				reference.getName(),
				reference.getType().name(),
				reference.getUpdatedAt()
		);
	}
}
