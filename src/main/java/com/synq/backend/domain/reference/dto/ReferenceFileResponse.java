package com.synq.backend.domain.reference.dto;

import com.synq.backend.domain.reference.entity.ReferenceMaterial;
import com.synq.backend.domain.user.entity.User;

import java.time.LocalDateTime;

public record ReferenceFileResponse(
		Long referenceId,
		String type,
		String name,
		Long fileSize,
		String fileExtension,
		String status,
		Long uploaderId,
		String uploaderName,
		LocalDateTime createdAt
) {
	public static ReferenceFileResponse from(ReferenceMaterial reference, User uploader) {
		return new ReferenceFileResponse(
				reference.getId(),
				reference.getType().name(),
				reference.getName(),
				reference.getFileSize(),
				reference.getFileExtension().name(),
				reference.getStatus().name(),
				reference.getUploaderId(),
				uploader.getName(),
				reference.getCreatedAt()
		);
	}
}
