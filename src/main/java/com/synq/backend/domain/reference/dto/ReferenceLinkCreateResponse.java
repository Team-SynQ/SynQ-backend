package com.synq.backend.domain.reference.dto;

import com.synq.backend.domain.reference.entity.ReferenceMaterial;
import com.synq.backend.domain.user.entity.User;

import java.time.LocalDateTime;

public record ReferenceLinkCreateResponse(
		Long referenceId,
		String type,
		String name,
		String url,
		String status,
		Long uploaderId,
		String uploaderName,
		LocalDateTime createdAt
) {
	public static ReferenceLinkCreateResponse from(ReferenceMaterial reference, User uploader) {
		return new ReferenceLinkCreateResponse(
				reference.getId(),
				reference.getType().name(),
				reference.getName(),
				reference.getUrl(),
				reference.getStatus().name(),
				reference.getUploaderId(),
				uploader.getName(),
				reference.getCreatedAt()
		);
	}
}
