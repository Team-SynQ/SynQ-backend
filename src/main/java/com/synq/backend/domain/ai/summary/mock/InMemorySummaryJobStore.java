package com.synq.backend.domain.ai.summary.mock;

import com.synq.backend.domain.ai.summary.domain.SummaryJob;
import com.synq.backend.domain.ai.summary.domain.SummaryJobStatus;
import com.synq.backend.domain.ai.summary.domain.SummaryJobStore;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "test"})
public class InMemorySummaryJobStore implements SummaryJobStore {

	// #23 Mock 단계의 임시 저장소다. 실제 구현에서는 ai_summary_job 테이블 어댑터로 교체한다.
	private final Map<UUID, SummaryJob> jobs = new ConcurrentHashMap<>();

	@Override
	public SummaryJob save(SummaryJob job) {
		jobs.put(job.id(), job);
		return job;
	}

	@Override
	public Optional<SummaryJob> findById(UUID jobId) {
		return Optional.ofNullable(jobs.get(jobId));
	}

	@Override
	public Optional<SummaryJob> findActiveByMeetingId(Long meetingId) {
		return jobs.values().stream()
				.filter(job -> job.meetingId().equals(meetingId))
				.filter(job -> job.status() == SummaryJobStatus.QUEUED
						|| job.status() == SummaryJobStatus.PROCESSING)
				.findFirst();
	}
}
