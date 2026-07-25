package com.synq.backend.domain.ai.summary.application;

import com.synq.backend.domain.ai.summary.code.SummaryErrorCode;
import com.synq.backend.domain.ai.summary.domain.MeetingStatusReader;
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
	private final MeetingStatusReader meetingStatusReader;

	public MeetingSummaryService(
			SummaryJobStore jobStore,
			MeetingSummaryStore summaryStore,
			SummaryJobProcessor processor,
			MeetingStatusReader meetingStatusReader
	) {
		this.jobStore = jobStore;
		this.summaryStore = summaryStore;
		this.processor = processor;
		this.meetingStatusReader = meetingStatusReader;
	}

	public synchronized SummaryJob request(Long meetingId) {
		// 종료되지 않은(진행 중이거나 존재하지 않는) 회의는 요약을 시작할 수 없다.
		if (!meetingStatusReader.isEnded(meetingId)) {
			throw new GeneralException(SummaryErrorCode.MEETING_NOT_ENDED);
		}
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
