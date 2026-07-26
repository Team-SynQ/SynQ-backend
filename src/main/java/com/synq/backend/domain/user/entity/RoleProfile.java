package com.synq.backend.domain.user.entity;

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

import java.time.OffsetDateTime;

@Entity
@Table(name = "role_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoleProfile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Role role;

	@Column(name = "detail_role", length = 30)
	private String detailRole;

	@Column(name = "is_default", nullable = false)
	private boolean isDefault;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private OffsetDateTime createdAt;

	private RoleProfile(Long userId, Role role, String detailRole, boolean isDefault) {
		this.userId = userId;
		this.role = role;
		this.detailRole = detailRole;
		this.isDefault = isDefault;
	}

	public static RoleProfile of(Long userId, Role role, String detailRole, boolean isDefault) {
		return new RoleProfile(userId, role, detailRole, isDefault);
	}

	public void update(Role role, String detailRole) {
		this.role = role;
		this.detailRole = detailRole;
	}

	public void markAsDefault() {
		this.isDefault = true;
	}

	public void unmarkAsDefault() {
		this.isDefault = false;
	}
}
