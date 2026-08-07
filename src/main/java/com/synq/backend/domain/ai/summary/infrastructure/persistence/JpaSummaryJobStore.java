package com.synq.backend.domain.ai.summary.infrastructure.persistence;

import com.synq.backend.domain.ai.summary.domain.SummaryJob;
import com.synq.backend.domain.ai.summary.domain.SummaryJobStatus;
import com.synq.backend.domain.ai.summary.domain.SummaryJobStore;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaSummaryJobStore implements SummaryJobStore {

	private static final List<SummaryJobStatus> ACTIVE_STATUSES =
			List.of(SummaryJobStatus.QUEUED, SummaryJobStatus.PROCESSING);

	private final AiSummaryJobJpaRepository repository;

	public JpaSummaryJobStore(AiSummaryJobJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional
	public SummaryJob save(SummaryJob job) {
		if (job.status() != SummaryJobStatus.QUEUED) {
			throw new IllegalArgumentException("새 요약 작업은 QUEUED 상태로만 저장할 수 있습니다.");
		}
		return repository.save(AiSummaryJobEntity.from(job)).toDomain();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<SummaryJob> findById(UUID jobId) {
		return repository.findById(jobId).map(AiSummaryJobEntity::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<SummaryJob> findActiveByMeetingId(Long meetingId) {
		return repository.findFirstByMeetingIdAndStatusInOrderByCreatedAtDesc(meetingId, ACTIVE_STATUSES)
				.map(AiSummaryJobEntity::toDomain);
	}

	@Override
	@Transactional
	public Optional<SummaryJob> startIfQueued(UUID jobId) {
		int updated = repository.startIfQueued(
				jobId,
				SummaryJobStatus.QUEUED,
				SummaryJobStatus.PROCESSING,
				Instant.now()
		);
		if (updated == 0) {
			return Optional.empty();
		}
		return repository.findById(jobId).map(AiSummaryJobEntity::toDomain);
	}

	@Override
	@Transactional
	public boolean failIfActive(UUID jobId, String errorMessage) {
		return repository.failIfActive(
				jobId,
				ACTIVE_STATUSES,
				SummaryJobStatus.FAILED,
				errorMessage,
				Instant.now()
		) == 1;
	}

	@Override
	@Transactional
	public boolean completeIfProcessing(UUID jobId, int failedPersonalSummaryCount) {
		if (failedPersonalSummaryCount < 0) {
			throw new IllegalArgumentException("개인 요약 실패 건수는 음수일 수 없습니다.");
		}
		SummaryJobStatus completedStatus = failedPersonalSummaryCount == 0
				? SummaryJobStatus.COMPLETED
				: SummaryJobStatus.COMPLETED_WITH_ERRORS;
		return repository.completeIfProcessing(
				jobId,
				SummaryJobStatus.PROCESSING,
				completedStatus,
				failedPersonalSummaryCount,
				Instant.now()
		) == 1;
	}
}
