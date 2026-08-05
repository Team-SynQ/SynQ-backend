package com.synq.backend.domain.reference.entity;

import com.synq.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "reference_material")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReferenceMaterial extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "project_id", nullable = false)
	private Long projectId;

	@Column(name = "uploader_id", nullable = false)
	private Long uploaderId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private ReferenceType type;

	@Column(nullable = false, length = 255)
	private String name;

	@Column(length = 2000)
	private String url;

	@Column(name = "file_size")
	private Long fileSize;

	@Enumerated(EnumType.STRING)
	@Column(name = "file_extension", length = 10)
	private ReferenceFileExtension fileExtension;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ReferenceStatus status;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	private ReferenceMaterial(
			Long projectId,
			Long uploaderId,
			ReferenceType type,
			String name,
			String url,
			Long fileSize,
			ReferenceFileExtension fileExtension,
			ReferenceStatus status
	) {
		this.projectId = projectId;
		this.uploaderId = uploaderId;
		this.type = type;
		this.name = name;
		this.url = url;
		this.fileSize = fileSize;
		this.fileExtension = fileExtension;
		this.status = status;
	}

	public static ReferenceMaterial ofFile(
			Long projectId,
			Long uploaderId,
			String name,
			Long fileSize,
			ReferenceFileExtension fileExtension,
			ReferenceStatus status
	) {
		return new ReferenceMaterial(
				projectId,
				uploaderId,
				ReferenceType.FILE,
				name,
				null,
				fileSize,
				fileExtension,
				status
		);
	}

	public static ReferenceMaterial ofLink(
			Long projectId,
			Long uploaderId,
			String name,
			String url,
			ReferenceStatus status
	) {
		return new ReferenceMaterial(
				projectId,
				uploaderId,
				ReferenceType.LINK,
				name,
				url,
				null,
				null,
				status
		);
	}

	public void softDelete() {
		this.deletedAt = LocalDateTime.now();
	}

	/** 청킹·임베딩까지 끝나 AI 가 참고할 수 있는 상태. */
	public void markAvailable() {
		this.status = ReferenceStatus.AVAILABLE;
	}

	/** 본문을 읽지 못했거나 인덱싱이 실패한 상태. 사용자에게 그대로 노출된다. */
	public void markReadFailed() {
		this.status = ReferenceStatus.READ_FAILED;
	}
}
