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
@Table(name = "role_profile_perspectives")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoleProfilePerspective {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "role_profile_id", nullable = false)
	private Long roleProfileId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Perspective perspective;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private OffsetDateTime createdAt;

	private RoleProfilePerspective(Long roleProfileId, Perspective perspective) {
		this.roleProfileId = roleProfileId;
		this.perspective = perspective;
	}

	public static RoleProfilePerspective of(Long roleProfileId, Perspective perspective) {
		return new RoleProfilePerspective(roleProfileId, perspective);
	}
}
