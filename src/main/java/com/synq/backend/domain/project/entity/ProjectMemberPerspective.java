package com.synq.backend.domain.project.entity;

import com.synq.backend.domain.user.entity.Perspective;
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

@Entity
@Table(name = "project_member_perspectives")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectMemberPerspective extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "project_member_id", nullable = false)
	private Long projectMemberId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Perspective perspective;

	private ProjectMemberPerspective(Long projectMemberId, Perspective perspective) {
		this.projectMemberId = projectMemberId;
		this.perspective = perspective;
	}

	public static ProjectMemberPerspective of(Long projectMemberId, Perspective perspective) {
		return new ProjectMemberPerspective(projectMemberId, perspective);
	}
}
