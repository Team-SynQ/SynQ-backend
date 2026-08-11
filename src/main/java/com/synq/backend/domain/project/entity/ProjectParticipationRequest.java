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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "project_join_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectParticipationRequest extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "project_id", nullable = false)
	private Long projectId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ProjectJoinRequestStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "setting_source", nullable = false, length = 20)
	private ProjectJoinSettingSource settingSource;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Role role;

	@Column(name = "detail_role", length = 30)
	private String detailRole;

	@Column(name = "requested_at", nullable = false)
	private OffsetDateTime requestedAt;

	private ProjectParticipationRequest(
			Long projectId,
			Long userId,
			ProjectJoinSettingSource settingSource,
			Role role,
			String detailRole
	) {
		this.projectId = projectId;
		this.userId = userId;
		this.status = ProjectJoinRequestStatus.PENDING;
		this.settingSource = settingSource;
		this.role = role;
		this.detailRole = detailRole;
		this.requestedAt = OffsetDateTime.now(ZoneOffset.UTC);
	}

	public static ProjectParticipationRequest pending(
			Long projectId,
			Long userId,
			ProjectJoinSettingSource settingSource,
			Role role,
			String detailRole
	) {
		return new ProjectParticipationRequest(projectId, userId, settingSource, role, detailRole);
	}
}
