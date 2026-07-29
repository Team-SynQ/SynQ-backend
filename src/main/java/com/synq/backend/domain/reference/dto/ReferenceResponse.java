package com.synq.backend.domain.reference.dto;

import com.synq.backend.domain.reference.entity.ReferenceMaterial;
import com.synq.backend.domain.user.entity.User;

import java.time.LocalDateTime;

public record ReferenceResponse(
		Long referenceId,
		String type,
		String name,
		String url,
		Long fileSize,
		String fileExtension,
		String status,
		Long uploaderId,
		String uploaderName,
		Boolean canDelete,
		LocalDateTime createdAt
) {
	public static ReferenceResponse from(
			ReferenceMaterial reference,
			User uploader,
			boolean canDelete
	) {
		return new ReferenceResponse(
				reference.getId(),
				reference.getType().name(),
				reference.getName(),
				reference.getUrl(),
				reference.getFileSize(),
				reference.getFileExtension() == null
						? null
						: reference.getFileExtension().name(),
				reference.getStatus().name(),
				reference.getUploaderId(),
				uploader.getName(),
				canDelete,
				reference.getCreatedAt()
		);
	}
}
