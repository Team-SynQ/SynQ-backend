package com.synq.backend.domain.ai.summary.domain;

import com.synq.backend.domain.ai.summary.domain.SummaryJob;
import java.util.Optional;
import java.util.UUID;

public interface SummaryJobStore {
	SummaryJob save(SummaryJob job);

	Optional<SummaryJob> findById(UUID jobId);
}
