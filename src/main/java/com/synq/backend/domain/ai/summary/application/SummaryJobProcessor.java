package com.synq.backend.domain.ai.summary.application;

import com.synq.backend.domain.ai.event.SummaryCompletedEvent;
import com.synq.backend.domain.ai.event.SummaryFailedEvent;
import com.synq.backend.domain.ai.summary.domain.MeetingSummary;
import com.synq.backend.domain.ai.summary.domain.SummaryJob;
import com.synq.backend.domain.ai.summary.domain.MeetingSummaryStore;
import com.synq.backend.domain.ai.summary.domain.SummaryAiClient;
import com.synq.backend.domain.ai.summary.domain.SummaryJobStore;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class SummaryJobProcessor {

	private final SummaryJobStore jobStore;
	private final MeetingSummaryStore summaryStore;
	private final SummaryContextBuilder contextBuilder;
	private final SummaryAiClient summaryAiClient;
	private final ApplicationEventPublisher eventPublisher;

	public SummaryJobProcessor(
			SummaryJobStore jobStore,
			MeetingSummaryStore summaryStore,
			SummaryContextBuilder contextBuilder,
			SummaryAiClient summaryAiClient,
			ApplicationEventPublisher eventPublisher
	) {
		this.jobStore = jobStore;
		this.summaryStore = summaryStore;
		this.contextBuilder = contextBuilder;
		this.summaryAiClient = summaryAiClient;
		this.eventPublisher = eventPublisher;
	}

	@Async("summaryExecutor")
	public void processAsync(UUID jobId) {
		// Controller 요청 스레드를 점유하지 않도록 요약 생성은 전용 Executor에서 실행한다.
		process(jobId);
	}

	public void process(UUID jobId) {
		SummaryJob job = jobStore.findById(jobId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 요약 작업입니다."));
		SummaryJob startedJob = jobStore.save(job.start());

		try {
			// Context 조합과 AI 호출을 분리해, 나중에 Reader나 AI 제공자를 독립적으로 교체할 수 있다.
			var context = contextBuilder.build(startedJob.meetingId());
			var generated = summaryAiClient.generate(context);
			summaryStore.save(MeetingSummary.from(startedJob.meetingId(), generated));
			jobStore.save(startedJob.complete());
			// 회의 상태(SUMMARIZED)를 확정하도록 meeting 도메인에 결과를 알린다.
			eventPublisher.publishEvent(new SummaryCompletedEvent(startedJob.meetingId()));
		} catch (Exception e) {
			// 비동기 예외가 호출자에게 전파되지 않으므로 실패 상태와 원인을 Job에 남긴다.
			String errorMessage = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
			jobStore.save(startedJob.fail(errorMessage));
			eventPublisher.publishEvent(new SummaryFailedEvent(startedJob.meetingId(), errorMessage));
		}
	}
}
