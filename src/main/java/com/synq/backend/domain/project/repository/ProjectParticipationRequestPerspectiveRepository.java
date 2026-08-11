package com.synq.backend.domain.project.repository;

import com.synq.backend.domain.project.entity.ProjectParticipationRequestPerspective;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectParticipationRequestPerspectiveRepository
		extends JpaRepository<ProjectParticipationRequestPerspective, Long> {

	List<ProjectParticipationRequestPerspective> findAllByJoinRequestIdOrderByIdAsc(Long joinRequestId);
}
