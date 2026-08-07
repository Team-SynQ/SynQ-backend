package com.synq.backend.domain.ai.summary.infrastructure.persistence;

import com.synq.backend.domain.ai.summary.domain.SummaryJobStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiSummaryJobJpaRepository extends JpaRepository<AiSummaryJobEntity, UUID> {

	Optional<AiSummaryJobEntity> findFirstByMeetingIdAndStatusInOrderByCreatedAtDesc(
			Long meetingId,
			Collection<SummaryJobStatus> statuses
	);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			UPDATE AiSummaryJobEntity job
			SET job.status = :processing, job.startedAt = :startedAt,
				job.completedAt = NULL, job.errorMessage = NULL
			WHERE job.id = :jobId AND job.status = :queued
			""")
	int startIfQueued(
			@Param("jobId") UUID jobId,
			@Param("queued") SummaryJobStatus queued,
			@Param("processing") SummaryJobStatus processing,
			@Param("startedAt") Instant startedAt
	);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			UPDATE AiSummaryJobEntity job
			SET job.status = :completed, job.completedAt = :completedAt,
				job.failedPersonalSummaryCount = :failedPersonalSummaryCount, job.errorMessage = NULL
			WHERE job.id = :jobId AND job.status = :processing
			""")
	int completeIfProcessing(
			@Param("jobId") UUID jobId,
			@Param("processing") SummaryJobStatus processing,
			@Param("completed") SummaryJobStatus completed,
			@Param("failedPersonalSummaryCount") int failedPersonalSummaryCount,
			@Param("completedAt") Instant completedAt
	);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			UPDATE AiSummaryJobEntity job
			SET job.status = :failed, job.errorMessage = :errorMessage, job.completedAt = :completedAt
			WHERE job.id = :jobId AND job.status IN :activeStatuses
			""")
	int failIfActive(
			@Param("jobId") UUID jobId,
			@Param("activeStatuses") Collection<SummaryJobStatus> activeStatuses,
			@Param("failed") SummaryJobStatus failed,
			@Param("errorMessage") String errorMessage,
			@Param("completedAt") Instant completedAt
	);
}
