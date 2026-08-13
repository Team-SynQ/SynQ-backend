package com.synq.backend.domain.project.repository;

import com.synq.backend.domain.project.entity.ProjectJoinRequestStatus;
import com.synq.backend.domain.project.entity.ProjectParticipationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
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

	@Query("""
			SELECT request
			FROM ProjectParticipationRequest request
			JOIN Project project ON project.id = request.projectId
			WHERE request.userId = :userId
			  AND request.status IN :statuses
			  AND project.deletedAt IS NULL
			ORDER BY request.updatedAt DESC, request.id DESC
			""")
	List<ProjectParticipationRequest> findAllProcessedByUserId(
			@Param("userId") Long userId,
			@Param("statuses") Collection<ProjectJoinRequestStatus> statuses
	);

	Optional<ProjectParticipationRequest> findByIdAndProjectId(Long requestId, Long projectId);
}
