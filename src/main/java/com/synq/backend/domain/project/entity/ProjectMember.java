package com.synq.backend.domain.project.entity;

import com.synq.backend.domain.user.entity.Role;
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

	@Column(name = "use_default", nullable = false)
	private boolean useDefault = true;

	@Enumerated(EnumType.STRING)
	@Column(name = "role_category", length = 30)
	private Role roleCategory;

	@Column(name = "detail_role", length = 30)
	private String detailRole;

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

	public void updateRolePerspective(boolean useDefault, Role roleCategory, String detailRole) {
		this.useDefault = useDefault;
		this.roleCategory = roleCategory;
		this.detailRole = detailRole;
	}
}
