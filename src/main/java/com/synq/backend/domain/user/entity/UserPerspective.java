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
@Table(name = "user_perspectives")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPerspective {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Perspective perspective;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private OffsetDateTime createdAt;

	private UserPerspective(Long userId, Perspective perspective) {
		this.userId = userId;
		this.perspective = perspective;
	}

	public static UserPerspective of(Long userId, Perspective perspective) {
		return new UserPerspective(userId, perspective);
	}
}
