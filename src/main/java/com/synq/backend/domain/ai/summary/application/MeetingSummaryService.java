package com.synq.backend.domain.ai.summary.application;

import com.synq.backend.domain.ai.summary.domain.MeetingSummary;
import com.synq.backend.domain.ai.summary.domain.SummaryJob;
import com.synq.backend.domain.ai.summary.domain.MeetingSummaryStore;
import com.synq.backend.domain.ai.summary.domain.SummaryJobStore;
import com.synq.backend.global.apipayload.code.GeneralErrorCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import java.util.UUID;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

@Service
public class MeetingSummaryService {

	private final SummaryJobStore jobStore;
	private final MeetingSummaryStore summaryStore;
	private final SummaryJobProcessor processor;

	public MeetingSummaryService(
			SummaryJobStore jobStore,
			MeetingSummaryStore summaryStore,
			SummaryJobProcessor processor
	) {
		this.jobStore = jobStore;
		this.summaryStore = summaryStore;
		this.processor = processor;
	}

	public synchronized SummaryJob request(Long meetingId) {
		jobStore.findActiveByMeetingId(meetingId).ifPresent(job -> {
			throw new GeneralException(GeneralErrorCode.CONFLICT);
		});

		SummaryJob job = jobStore.save(SummaryJob.queued(meetingId));
		// 요청은 접수만 하고, 실제 생성은 비동기 Processor가 이어서 수행한다.
		try {
			processor.processAsync(job.id());
		} catch (TaskRejectedException e) {
			jobStore.save(job.fail("요약 작업 대기열이 가득 찼습니다."));
			throw new GeneralException(GeneralErrorCode.SERVICE_UNAVAILABLE);
		}
		return job;
	}

	public SummaryJob getJob(Long meetingId, UUID jobId) {
		SummaryJob job = jobStore.findById(jobId)
				.orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND));
		if (!job.meetingId().equals(meetingId)) {
			// 다른 회의의 Job ID로 상태를 조회하는 것을 막는다.
			throw new GeneralException(GeneralErrorCode.NOT_FOUND);
		}
		return job;
	}

	public MeetingSummary getLatestSummary(Long meetingId) {
		return summaryStore.findLatestByMeetingId(meetingId)
				.orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND));
	}
}
