package com.synq.backend.domain.project.entity;

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

import java.time.LocalDateTime;

@Entity
@Table(name = "project_member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectMember extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "project_id", nullable = false)
	private Long projectId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private ProjectMemberRole role;

	@Column(name = "joined_at", nullable = false)
	private LocalDateTime joinedAt;

	private ProjectMember(Long projectId, Long userId, ProjectMemberRole role, LocalDateTime joinedAt) {
		this.projectId = projectId;
		this.userId = userId;
		this.role = role;
		this.joinedAt = joinedAt;
	}

	public static ProjectMember of(Long projectId, Long userId, ProjectMemberRole role) {
		return new ProjectMember(projectId, userId, role, LocalDateTime.now());
	}
}
