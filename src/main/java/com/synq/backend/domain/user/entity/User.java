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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private Long userId;

	@Column(nullable = false, length = 20)
	private String name;

	@Column(nullable = false)
	private String email;

	// LOCAL(dev 전용 이메일 로그인)에서만 사용. 소셜 로그인 유저는 null.
	@Column(name = "password_hash")
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Provider provider;

	// LOCAL 은 provider_id 없음
	@Column(name = "provider_id")
	private String providerId;

	@Enumerated(EnumType.STRING)
	@Column(length = 30)
	private Role role;

	@Column(name = "detail_role", length = 30)
	private String detailRole;

	@Column(name = "onboarding_completed_at")
	private OffsetDateTime onboardingCompletedAt;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	private User(String name, String email, String passwordHash, Provider provider, String providerId) {
		this.name = name;
		this.email = email;
		this.passwordHash = passwordHash;
		this.provider = provider;
		this.providerId = providerId;
	}

	public static User ofSocial(String name, String email, Provider provider, String providerId) {
		return new User(name, email, null, provider, providerId);
	}

	public static User ofLocal(String name, String email, String passwordHash) {
		return new User(name, email, passwordHash, Provider.LOCAL, null);
	}

	public boolean isOnboarded() {
		return onboardingCompletedAt != null;
	}
}
