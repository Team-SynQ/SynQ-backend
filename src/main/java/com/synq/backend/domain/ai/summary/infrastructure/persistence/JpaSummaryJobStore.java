package com.synq.backend.domain.ai.summary.infrastructure.persistence;

import com.synq.backend.domain.ai.summary.domain.SummaryJob;
import com.synq.backend.domain.ai.summary.domain.SummaryJobStatus;
import com.synq.backend.domain.ai.summary.domain.SummaryJobStore;
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
		AiSummaryJobEntity entity = repository.findById(job.id())
				.orElseGet(() -> AiSummaryJobEntity.from(job));
		entity.apply(job);
		return repository.save(entity).toDomain();
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
}
