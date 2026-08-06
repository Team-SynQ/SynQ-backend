package com.synq.backend.domain.ai.summary.application;

import com.synq.backend.domain.ai.event.SummaryCompletedEvent;
import com.synq.backend.domain.ai.event.SummaryFailedEvent;
import com.synq.backend.domain.ai.summary.domain.GeneratedPersonalSummary;
import com.synq.backend.domain.ai.summary.domain.GeneratedSummary;
import com.synq.backend.domain.ai.summary.domain.PersonalSummaryAiClient;
import com.synq.backend.domain.ai.summary.domain.PersonalSummaryTarget;
import com.synq.backend.domain.ai.summary.domain.PersonalSummaryTargetReader;
import com.synq.backend.domain.ai.summary.domain.SummaryAiClient;
import com.synq.backend.domain.ai.summary.domain.SummaryContext;
import com.synq.backend.domain.ai.summary.domain.SummaryJob;
import com.synq.backend.domain.ai.summary.domain.SummaryJobStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class SummaryJobProcessor {

	static final String SUMMARY_GENERATION_FAILED_MESSAGE = "AI 요약 생성에 실패했습니다.";
	private static final int MAX_REDUCTION_ROUNDS = 8;
	private static final Logger log = LoggerFactory.getLogger(SummaryJobProcessor.class);

	private final SummaryJobStore jobStore;
	private final SummaryContextBuilder contextBuilder;
	private final SummaryAiClient summaryAiClient;
	private final PersonalSummaryAiClient personalSummaryAiClient;
	private final PersonalSummaryTargetReader personalSummaryTargetReader;
	private final SummaryResultWriter resultWriter;
	private final SummaryProperties properties;
	private final ApplicationEventPublisher eventPublisher;

	public SummaryJobProcessor(
			SummaryJobStore jobStore,
			SummaryContextBuilder contextBuilder,
			SummaryAiClient summaryAiClient,
			PersonalSummaryAiClient personalSummaryAiClient,
			PersonalSummaryTargetReader personalSummaryTargetReader,
			SummaryResultWriter resultWriter,
			SummaryProperties properties,
			ApplicationEventPublisher eventPublisher
	) {
		this.jobStore = jobStore;
		this.contextBuilder = contextBuilder;
		this.summaryAiClient = summaryAiClient;
		this.personalSummaryAiClient = personalSummaryAiClient;
		this.personalSummaryTargetReader = personalSummaryTargetReader;
		this.resultWriter = resultWriter;
		this.properties = properties;
		this.eventPublisher = eventPublisher;
	}

	@Async("summaryExecutor")
	public void processAsync(UUID jobId) {
		// Controller 요청 스레드를 점유하지 않도록 요약 생성은 전용 Executor에서 실행한다.
		process(jobId);
	}

	public void process(UUID jobId) {
		SummaryJob startedJob = jobStore.startIfQueued(jobId).orElse(null);
		if (startedJob == null) {
			// 만료 또는 취소된 Job은 지연 실행되더라도 결과를 만들지 않는다.
			log.info("요약 작업을 시작하지 않습니다. 이미 종료되었거나 시작된 Job입니다. jobId={}", jobId);
			return;
		}

		boolean succeeded = false;
		boolean failureRecorded = false;
		String errorMessage = null;
		try {
			// Context 조합과 AI 호출을 분리해, 나중에 Reader나 AI 제공자를 독립적으로 교체할 수 있다.
			var context = contextBuilder.build(startedJob.meetingId());
			var generation = generateOverall(context);
			var generated = generation.summary();
			var targets = personalSummaryTargetReader.findByMeetingId(startedJob.meetingId());
			var generatedPersonalSummaries = new ArrayList<SummaryResultWriter.PersonalGeneration>();
			int failedPersonalSummaryCount = 0;
			for (var target : targets) {
				Optional<GeneratedPersonalSummary> personalSummary = generatePersonalSummary(
						generation.personalContext(), generated, target, startedJob.meetingId(), startedJob.id());
				if (personalSummary.isPresent()) {
					generatedPersonalSummaries.add(
							new SummaryResultWriter.PersonalGeneration(target, personalSummary.orElseThrow()));
				} else {
					failedPersonalSummaryCount++;
				}
			}
			if (failedPersonalSummaryCount > 0) {
				log.warn("개인 요약 생성이 최종 실패했습니다. meetingId={}, jobId={}, failedPersonalSummaryCount={}",
						startedJob.meetingId(), startedJob.id(), failedPersonalSummaryCount);
			}

			succeeded = resultWriter.saveIfJobProcessing(
					startedJob,
					generated,
					generatedPersonalSummaries,
					failedPersonalSummaryCount
			);
			if (!succeeded) {
				log.info("요약 작업 결과를 저장하지 않습니다. 이미 종료된 Job입니다. jobId={}", startedJob.id());
			}
		} catch (Exception e) {
			log.error("AI 요약 생성에 실패했습니다. meetingId={}, jobId={}",
					startedJob.meetingId(), startedJob.id(), e);
			// 제공자 응답과 내부 예외는 로그에만 남기고 API에는 고정된 안전한 메시지를 제공한다.
			errorMessage = SUMMARY_GENERATION_FAILED_MESSAGE;
			failureRecorded = jobStore.failIfActive(startedJob.id(), errorMessage);
			if (!failureRecorded) {
				log.info("요약 작업 실패 상태를 저장하지 않습니다. 이미 종료된 Job입니다. jobId={}", startedJob.id());
			}
		}

		// Job 상태가 확정된 뒤에 결과를 알린다. 이벤트를 try 안에서 발행하면 구독자(회의 상태 반영) 실패가
		// catch 로 전파돼 방금 저장한 COMPLETED Job 을 FAILED 로 덮어쓸 수 있어, 발행은 반드시 밖에서 한다.
		if (succeeded) {
			eventPublisher.publishEvent(new SummaryCompletedEvent(startedJob.meetingId(), startedJob.id()));
		} else if (failureRecorded) {
			// 내부 예외 상세는 Job에만 보관하고, SSE에는 사용자에게 안전한 메시지만 전달한다.
			eventPublisher.publishEvent(new SummaryFailedEvent(
					startedJob.meetingId(),
					startedJob.id(),
					SUMMARY_GENERATION_FAILED_MESSAGE
			));
		}
	}

	private Optional<GeneratedPersonalSummary> generatePersonalSummary(
			SummaryContext context,
			GeneratedSummary overall,
			PersonalSummaryTarget target,
			Long meetingId,
			UUID jobId
	) {
		for (int attempt = 1; attempt <= 2; attempt++) {
			try {
				return Optional.of(personalSummaryAiClient.generate(context, overall, target));
			} catch (Exception e) {
				if (attempt == 2) {
					log.warn("개인 요약 생성 재시도까지 실패했습니다. meetingId={}, jobId={}, retryCount={}",
							meetingId, jobId, attempt, e);
				} else {
					log.warn("개인 요약 생성에 실패해 재시도합니다. meetingId={}, jobId={}, retryCount={}",
							meetingId, jobId, attempt, e);
				}
			}
		}
		return Optional.empty();
	}

	private OverallGeneration generateOverall(SummaryContext context) {
		if (contextSize(context) <= properties.maxInputChars()) {
			return new OverallGeneration(summaryAiClient.generate(context), context);
		}

		String reducedTranscript = context.transcript();
		int reductionRound = 0;
		while (reducedTranscript.length() + referenceContextChars(context) > properties.maxInputChars()) {
			if (++reductionRound > MAX_REDUCTION_ROUNDS) {
				throw new IllegalStateException("회의 전사를 입력 제한 이하로 축약하지 못했습니다.");
			}

			List<String> partialSummaries = split(reducedTranscript, properties.maxInputChars()).stream()
					.map(chunk -> reduceChunk(context.meetingId(), chunk))
					.toList();
			// 부분 요약은 최종 응답이 아닌 다음 AI 호출을 위한 중간 컨텍스트다. 모델이 길게
			// 응답하더라도 원문보다 작게 제한해 축약 단계가 실패하거나 무한 반복되지 않게 한다.
			reducedTranscript = abbreviate(String.join("\n\n", partialSummaries), reducedTranscript.length() - 1);
		}

		var reducedContext = new SummaryContext(context.meetingId(), reducedTranscript, context.referenceContexts());
		return new OverallGeneration(summaryAiClient.generate(reducedContext), reducedContext);
	}

	private int contextSize(SummaryContext context) {
		return context.transcript().length() + referenceContextChars(context);
	}

	private int referenceContextChars(SummaryContext context) {
		return context.referenceContexts().stream().mapToInt(String::length).sum();
	}

	private List<String> split(String transcript, int maxChars) {
		List<String> chunks = new ArrayList<>();
		StringBuilder current = new StringBuilder();

		for (String line : transcript.split("\\R")) {
			if (line.length() > maxChars) {
				flush(chunks, current);
				for (int start = 0; start < line.length(); start += maxChars) {
					chunks.add(line.substring(start, Math.min(start + maxChars, line.length())));
				}
				continue;
			}
			if (!current.isEmpty() && current.length() + line.length() + 1 > maxChars) {
				flush(chunks, current);
			}
			if (!current.isEmpty()) {
				current.append('\n');
			}
			current.append(line);
		}
		flush(chunks, current);
		return chunks;
	}

	private void flush(List<String> chunks, StringBuilder current) {
		if (!current.isEmpty()) {
			chunks.add(current.toString());
			current.setLength(0);
		}
	}

	private String reduceChunk(Long meetingId, String chunk) {
		GeneratedSummary partialSummary = summaryAiClient.generate(new SummaryContext(meetingId, chunk, List.of()));
		return abbreviate(formatPartialSummary(partialSummary), Math.max(1, chunk.length() / 2));
	}

	private String abbreviate(String text, int maxChars) {
		if (text.length() <= maxChars) {
			return text;
		}
		if (maxChars == 1) {
			return "…";
		}
		return text.substring(0, maxChars - 1) + "…";
	}

	private String formatPartialSummary(GeneratedSummary summary) {
		List<String> parts = new ArrayList<>();
		parts.add("[한 줄 요약] " + summary.oneLineSummary());
		appendIfPresent(parts, "[주제] ", summary.keyTopics());
		summary.discussionSections().forEach(section ->
				parts.add("[주요 논의] " + section.title() + ": " + String.join(", ", section.details())));
		appendIfPresent(parts, "[결정] ", summary.decisions());
		appendIfPresent(parts, "[논의 방향] ", summary.tentativeDirections());
		appendIfPresent(parts, "[확인 필요] ", summary.confirmationItems());
		return String.join("\n", parts);
	}

	private void appendIfPresent(List<String> parts, String label, List<String> values) {
		if (!values.isEmpty()) {
			parts.add(label + String.join(", ", values));
		}
	}

	private record OverallGeneration(
			GeneratedSummary summary,
			SummaryContext personalContext
	) {
	}
}
