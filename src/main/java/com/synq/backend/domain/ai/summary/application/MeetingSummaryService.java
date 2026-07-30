package com.synq.backend.domain.ai.summary.application;

import com.synq.backend.domain.ai.summary.code.SummaryErrorCode;
import com.synq.backend.domain.ai.summary.domain.MeetingStatusReader;
import com.synq.backend.domain.ai.summary.domain.MeetingSummary;
import com.synq.backend.domain.ai.summary.domain.SummaryJob;
import com.synq.backend.domain.ai.summary.domain.MeetingSummaryStore;
import com.synq.backend.domain.ai.summary.domain.SummaryJobStore;
import com.synq.backend.global.apipayload.code.GeneralErrorCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class MeetingSummaryService {

	private static final String STALE_JOB_MESSAGE = "이전 요약 작업이 제한 시간을 초과했습니다.";

	private final SummaryJobStore jobStore;
	private final MeetingSummaryStore summaryStore;
	private final SummaryJobProcessor processor;
	private final MeetingStatusReader meetingStatusReader;
	private final SummaryProperties properties;
	private final SummaryAccessValidator accessValidator;

	public MeetingSummaryService(
			SummaryJobStore jobStore,
			MeetingSummaryStore summaryStore,
			SummaryJobProcessor processor,
			MeetingStatusReader meetingStatusReader,
			SummaryProperties properties,
			SummaryAccessValidator accessValidator
	) {
		this.jobStore = jobStore;
		this.summaryStore = summaryStore;
		this.processor = processor;
		this.meetingStatusReader = meetingStatusReader;
		this.properties = properties;
		this.accessValidator = accessValidator;
	}

	public SummaryJob request(Long meetingId, Long userId) {
		validateAccess(meetingId, userId);
		return requestInternal(meetingId);
	}

	/**
	 * 회의 종료 이벤트가 호출하는 내부 진입점이다. 외부 API는 사용자 검증이 포함된 오버로드를 사용한다.
	 */
	SummaryJob requestAfterMeetingEnd(Long meetingId) {
		return requestInternal(meetingId);
	}

	private synchronized SummaryJob requestInternal(Long meetingId) {
		// 종료되지 않은(진행 중이거나 존재하지 않는) 회의는 요약을 시작할 수 없다.
		if (!meetingStatusReader.isEnded(meetingId)) {
			throw new GeneralException(SummaryErrorCode.MEETING_NOT_ENDED);
		}
		jobStore.findActiveByMeetingId(meetingId).ifPresent(job -> {
			if (job.isStale(Instant.now(), properties.activeJobTimeout())) {
				jobStore.failIfActive(job.id(), STALE_JOB_MESSAGE);
				return;
			}
			throw new GeneralException(GeneralErrorCode.CONFLICT);
		});

		SummaryJob job;
		try {
			job = jobStore.save(SummaryJob.queued(
					meetingId,
					properties.modelName(),
					properties.promptVersion()
			));
		} catch (DataIntegrityViolationException e) {
			// 여러 서버가 동시에 요청해도 DB의 활성 Job 고유 인덱스로 중복 생성을 막는다.
			throw new GeneralException(GeneralErrorCode.CONFLICT, e);
		}
		// 요청은 접수만 하고, 실제 생성은 비동기 Processor가 이어서 수행한다.
		try {
			processor.processAsync(job.id());
		} catch (TaskRejectedException e) {
			jobStore.failIfActive(job.id(), "요약 작업 대기열이 가득 찼습니다.");
			throw new GeneralException(GeneralErrorCode.SERVICE_UNAVAILABLE);
		}
		return job;
	}

	private SummaryJob getJob(Long meetingId, UUID jobId) {
		SummaryJob job = jobStore.findById(jobId)
				.orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND));
		if (!job.meetingId().equals(meetingId)) {
			// 다른 회의의 Job ID로 상태를 조회하는 것을 막는다.
			throw new GeneralException(GeneralErrorCode.NOT_FOUND);
		}
		return job;
	}

	public SummaryJob getJob(Long meetingId, UUID jobId, Long userId) {
		validateAccess(meetingId, userId);
		return getJob(meetingId, jobId);
	}

	private MeetingSummary getLatestSummary(Long meetingId) {
		return summaryStore.findLatestByMeetingId(meetingId)
				.orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND));
	}

	public MeetingSummary getLatestSummary(Long meetingId, Long userId) {
		validateAccess(meetingId, userId);
		return getLatestSummary(meetingId);
	}

	public void validateAccess(Long meetingId, Long userId) {
		accessValidator.validateParticipant(meetingId, userId);
	}
}
