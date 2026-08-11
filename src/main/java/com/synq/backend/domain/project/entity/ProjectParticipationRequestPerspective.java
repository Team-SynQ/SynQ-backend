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
@Table(name = "project_join_request_perspectives")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectParticipationRequestPerspective extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "join_request_id", nullable = false)
	private Long joinRequestId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Perspective perspective;

	private ProjectParticipationRequestPerspective(Long joinRequestId, Perspective perspective) {
		this.joinRequestId = joinRequestId;
		this.perspective = perspective;
	}

	public static ProjectParticipationRequestPerspective of(Long joinRequestId, Perspective perspective) {
		return new ProjectParticipationRequestPerspective(joinRequestId, perspective);
	}
}
