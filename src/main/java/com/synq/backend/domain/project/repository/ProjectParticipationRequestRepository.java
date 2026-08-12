package com.synq.backend.domain.project.repository;

import com.synq.backend.domain.project.entity.ProjectJoinRequestStatus;
import com.synq.backend.domain.project.entity.ProjectParticipationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectParticipationRequestRepository extends JpaRepository<ProjectParticipationRequest, Long> {

	boolean existsByProjectIdAndUserIdAndStatus(
			Long projectId,
			Long userId,
			ProjectJoinRequestStatus status
	);

	Optional<ProjectParticipationRequest> findByProjectIdAndUserIdAndStatus(
			Long projectId,
			Long userId,
			ProjectJoinRequestStatus status
	);

	List<ProjectParticipationRequest> findAllByProjectIdAndUserIdOrderByRequestedAtAsc(
			Long projectId,
			Long userId
	);

	List<ProjectParticipationRequest> findAllByProjectIdAndStatusOrderByRequestedAtAscIdAsc(
			Long projectId,
			ProjectJoinRequestStatus status
	);

	Optional<ProjectParticipationRequest> findByIdAndProjectId(Long requestId, Long projectId);
}
