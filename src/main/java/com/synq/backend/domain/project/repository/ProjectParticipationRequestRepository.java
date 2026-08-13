package com.synq.backend.domain.project.repository;

import com.synq.backend.domain.project.entity.ProjectJoinRequestStatus;
import com.synq.backend.domain.project.entity.ProjectParticipationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
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

	@Query(value = """
			SELECT request.id AS requestId,
			       request.project_id AS projectId,
			       project.title AS projectTitle,
			       request.status AS status,
			       request.updated_at AT TIME ZONE 'Asia/Seoul' AS decidedAt
			FROM project_join_request request
			JOIN project project ON project.id = request.project_id
			WHERE request.user_id = :userId
			  AND request.status IN ('APPROVED', 'REJECTED')
			  AND project.deleted_at IS NULL
			ORDER BY request.updated_at DESC, request.id DESC
			""", nativeQuery = true)
	List<ProcessedJoinRequestView> findAllProcessedByUserId(@Param("userId") Long userId);

	Optional<ProjectParticipationRequest> findByIdAndProjectId(Long requestId, Long projectId);

	interface ProcessedJoinRequestView {
		Long getRequestId();

		Long getProjectId();

		String getProjectTitle();

		String getStatus();

		Instant getDecidedAt();
	}
}
