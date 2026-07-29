package com.synq.backend.domain.project.entity;

import com.synq.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "project")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "owner_id", nullable = false)
	private Long ownerId;

	@Column(nullable = false, length = 30)
	private String title;

	@Column(length = 500)
	private String description;

	@Column(name = "invite_token", length = 36, unique = true)
	private String inviteToken;

	@Column(name = "invite_token_expires_at")
	private LocalDateTime inviteTokenExpiresAt;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	private Project(Long ownerId, String title, String description) {
		this.ownerId = ownerId;
		this.title = title;
		this.description = description;
	}

	public static Project of(Long ownerId, String title, String description) {
		return new Project(ownerId, title, description);
	}

	public void updateTitle(String title) {
		this.title = title;
	}

	public void updateDescription(String description) {
		this.description = description;
	}

	public void updateInvitation(String inviteToken, LocalDateTime expiresAt) {
		this.inviteToken = inviteToken;
		this.inviteTokenExpiresAt = expiresAt;
	}

	public void softDelete() {
		this.deletedAt = LocalDateTime.now();
	}

	public boolean isDeleted() {
		return deletedAt != null;
	}
}
