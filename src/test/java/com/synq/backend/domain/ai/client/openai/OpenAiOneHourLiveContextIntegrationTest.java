package com.synq.backend.domain.ai.client.openai;

import static org.assertj.core.api.Assertions.assertThat;

import com.synq.backend.domain.ai.assistant.domain.SegmentHint;
import com.synq.backend.domain.ai.assistant.repository.SegmentHintRepository;
import com.synq.backend.domain.ai.context.domain.LiveContextSnapshot;
import com.synq.backend.domain.ai.context.repository.LiveContextRepository;
import com.synq.backend.domain.ai.event.AutoHintCreatedEvent;
import com.synq.backend.domain.ai.event.LiveContextUpdatedEvent;
import com.synq.backend.domain.meeting.entity.MeetingParticipant;
import com.synq.backend.domain.meeting.entity.ParticipantRole;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.domain.transcript.entity.TranscriptSegment;
import com.synq.backend.domain.transcript.event.TranscriptFinalizedEvent;
import com.synq.backend.domain.transcript.repository.TranscriptSegmentRepository;
import com.synq.backend.support.MeetingTranscriptTestFixture;
import com.synq.backend.support.TestPortConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 실제 OpenAI Responses API로 1시간 전사(20초 x 180개)를 처리하는 수동 평가다.
 *
 * <p>기본 테스트에서 외부 API 비용이 발생하지 않도록 명시적인 환경변수로만 실행한다.
 */
@SpringBootTest
@ActiveProfiles("local")
@Import({TestPortConfig.class, OpenAiOneHourLiveContextIntegrationTest.EventCaptureConfig.class})
@TestPropertySource(properties = {
		"spring.task.scheduling.enabled=false",
		"ai.live-context.client=openai",
		"ai.live-context.model=gpt-5.6-luna",
		"ai.live-context.reasoning-effort=low",
		"ai.live-context.max-output-tokens=1200",
		"ai.live-context.batch.segment-count=2",
		"ai.live-context.batch.max-segments-per-request=2",
		"ai.live-context.batch.max-delay=10s",
		"ai.assistant.client=openai",
		"ai.assistant.model=gpt-5.6-luna",
		"ai.assistant.reasoning-effort=low",
		"ai.assistant.max-output-tokens=1200",
		"ai.assistant.auto-hint.enabled=true",
		"ai.assistant.auto-hint.importance-threshold=60"
})
@EnabledIfEnvironmentVariable(named = "RUN_OPENAI_ONE_HOUR_INTEGRATION_TEST", matches = "true")
class OpenAiOneHourLiveContextIntegrationTest {

	private static final int SEGMENT_COUNT = 180;
	private static final int BATCH_SIZE = 2;
	private static final int EXPECTED_REFRESH_COUNT = SEGMENT_COUNT / BATCH_SIZE;
	private static final Path REPORT_PATH = Path.of(
			"docs/test-artifacts/ai-integration-2026-08-11/openai-one-hour-live-context-report.md");

	@Autowired
	private MeetingTranscriptTestFixture fixture;

	@Autowired
	private TranscriptSegmentRepository transcriptSegmentRepository;

	@Autowired
	private MeetingParticipantRepository participantRepository;

	@Autowired
	private LiveContextRepository liveContextRepository;

	@Autowired
	private SegmentHintRepository segmentHintRepository;

	@Autowired
	private ApplicationEventPublisher eventPublisher;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private EventCapture eventCapture;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void 실제_OpenAI로_한시간_전사의_Live_Context와_자동_힌트를_검증하고_보고서를_남긴다() {
		MeetingTranscriptTestFixture.Fixture meeting = fixture.create();
		try {
			participantRepository.saveAndFlush(MeetingParticipant.of(
					meeting.meetingId(), meeting.hostId(), ParticipantRole.HOST));

			Instant startedAt = Instant.now();
			for (int sequence = 0; sequence < SEGMENT_COUNT; sequence++) {
				publishFinalizedSegment(meeting.meetingId(), sequence);
				if ((sequence + 1) % BATCH_SIZE == 0) {
					int expectedSequence = sequence;
					await("Live Context %d/%d".formatted((sequence + 1) / BATCH_SIZE, EXPECTED_REFRESH_COUNT),
							Duration.ofMinutes(2),
							() -> liveContextRepository.findByMeetingId(meeting.meetingId())
									.map(context -> context.getLastSequenceIndex() == expectedSequence)
									.orElse(false));
				}
			}

			await("Live Context 전체 처리", Duration.ofMinutes(2),
					() -> eventCapture.liveContextsFor(meeting.meetingId()).size() == EXPECTED_REFRESH_COUNT);
			await("자동 힌트 최초 생성", Duration.ofMinutes(2), () ->
					!eventCapture.autoHintsFor(meeting.meetingId()).isEmpty()
							&& !segmentHintRepository.findByMeetingIdAndUserIdOrderBySegmentIdAsc(
							meeting.meetingId(), meeting.hostId()).isEmpty());
			waitForHintsToSettle(meeting.meetingId());

			List<TranscriptSegment> segments = transcriptSegmentRepository
					.findByMeetingIdAndSequenceIndexGreaterThanOrderByStartMsAscSequenceIndexAsc(meeting.meetingId(), -1);
			List<SegmentHint> hints = segmentHintRepository
					.findByMeetingIdAndUserIdOrderBySegmentIdAsc(meeting.meetingId(), meeting.hostId());
			List<LiveContextUpdatedEvent> updates = eventCapture.liveContextsFor(meeting.meetingId());

			assertThat(updates).hasSize(EXPECTED_REFRESH_COUNT);
			assertThat(eventCapture.autoHintsFor(meeting.meetingId())).isNotEmpty();
			assertThat(hints).isNotEmpty();
			assertThat(liveContextRepository.findByMeetingId(meeting.meetingId()).orElseThrow().getLastSequenceIndex())
					.isEqualTo(SEGMENT_COUNT - 1);
			writeReport(meeting.meetingId(), startedAt, segments, updates, hints);
		} finally {
			deleteFixture(meeting);
		}
	}

	private void deleteFixture(MeetingTranscriptTestFixture.Fixture fixture) {
		new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
			jdbcTemplate.update("DELETE FROM ai_segment_hint WHERE meeting_id = ?", fixture.meetingId());
			jdbcTemplate.update("DELETE FROM meeting_live_context WHERE meeting_id = ?", fixture.meetingId());
			jdbcTemplate.update("DELETE FROM transcript_segment WHERE meeting_id = ?", fixture.meetingId());
			jdbcTemplate.update("DELETE FROM meeting_participant WHERE meeting_id = ?", fixture.meetingId());
			jdbcTemplate.update("DELETE FROM meeting WHERE id = ?", fixture.meetingId());
			jdbcTemplate.update("DELETE FROM project WHERE id = ?", fixture.projectId());
			jdbcTemplate.update("DELETE FROM users WHERE user_id = ?", fixture.hostId());
		});
	}

	private void publishFinalizedSegment(Long meetingId, int sequence) {
		new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
			int startMs = sequence * 20_000;
			TranscriptSegment segment = transcriptSegmentRepository.saveAndFlush(TranscriptSegment.of(
					meetingId,
					sequence,
					startMs,
					startMs + 20_000,
					transcript(sequence)));
			eventPublisher.publishEvent(new TranscriptFinalizedEvent(
					meetingId,
					segment.getId(),
					sequence,
					startMs,
					startMs + 20_000,
					segment.getContent(),
					null));
		});
	}

	private String transcript(int sequence) {
		int batch = sequence / BATCH_SIZE + 1;
		String timestamp = "%02d:%02d".formatted(sequence / 3, (sequence % 3) * 20);
		if (sequence % BATCH_SIZE == BATCH_SIZE - 1 && batch % 10 == 0) {
			return ("%s 결정: 온보딩 개선안을 이번 분기 최우선 과제로 확정합니다. 민규는 API 구현을, 서윤은 "
					+ "QA 시나리오 검토를 담당하고 금요일까지 진행 상황을 공유합니다.")
					.formatted(timestamp);
		}
		if (sequence % BATCH_SIZE == BATCH_SIZE - 1 && batch % 7 == 0) {
			return ("%s 리스크: QA 인력 확보가 늦어지면 베타 일정이 밀릴 수 있습니다. 다음 회의에서 "
					+ "대체 일정과 범위를 확인해야 합니다.")
					.formatted(timestamp);
		}
		return "%s 진행 공유: 온보딩 개선 작업의 현황과 사용자 이탈률을 확인했고, 다음 작업 범위를 논의 중입니다."
				.formatted(timestamp);
	}

	private void waitForHintsToSettle(Long meetingId) {
		int previousSize = -1;
		int stableChecks = 0;
		for (int attempt = 0; attempt < 60; attempt++) {
			int currentSize = eventCapture.autoHintsFor(meetingId).size();
			if (currentSize == previousSize) {
				stableChecks++;
				if (stableChecks >= 5) {
					return;
				}
			} else {
				previousSize = currentSize;
				stableChecks = 0;
			}
			sleep(Duration.ofSeconds(1));
		}
	}

	private void await(String target, Duration timeout, BooleanSupplier condition) {
		Instant deadline = Instant.now().plus(timeout);
		while (Instant.now().isBefore(deadline)) {
			if (condition.getAsBoolean()) {
				return;
			}
			sleep(Duration.ofMillis(200));
		}
		throw new AssertionError(target + " 처리 시간이 초과되었습니다.");
	}

	private void sleep(Duration duration) {
		try {
			Thread.sleep(duration.toMillis());
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("평가 대기 중 인터럽트되었습니다.", exception);
		}
	}

	private void writeReport(
			Long meetingId,
			Instant startedAt,
			List<TranscriptSegment> segments,
			List<LiveContextUpdatedEvent> updates,
			List<SegmentHint> hints
	) {
		try {
			Files.createDirectories(REPORT_PATH.getParent());
			Files.writeString(REPORT_PATH, report(meetingId, startedAt, segments, updates, hints), StandardCharsets.UTF_8);
		} catch (IOException exception) {
			throw new IllegalStateException("OpenAI 통합 테스트 보고서를 저장하지 못했습니다.", exception);
		}
	}

	private String report(
			Long meetingId,
			Instant startedAt,
			List<TranscriptSegment> segments,
			List<LiveContextUpdatedEvent> updates,
			List<SegmentHint> hints
	) {
		LiveContextUpdatedEvent first = updates.get(0);
		LiveContextUpdatedEvent last = updates.get(updates.size() - 1);
		String hintSection = hints.isEmpty() ? "생성된 자동 힌트가 없습니다."
				: hints.stream().limit(3).map(this::formatHint).reduce((left, right) -> left + "\n\n" + right).orElseThrow();

		return """
				# 실제 OpenAI 1시간 Live Context·자동 힌트 통합 테스트

				## 실행 결과

				| 항목 | 결과 |
				| --- | --- |
				| 회의 ID | %d |
				| 전사 세그먼트 | %d개 (20초 간격, 총 60분) |
				| Live Context 갱신 | %d회 (2개 세그먼트 또는 최대 10초 대기) |
				| 자동 3-hint 저장 | %d개 |
				| Live Context 평균 지연 | %s |
				| Live Context 최대 지연 | %s |
				| 자동 3-hint 평균 표시 지연 | %s |
				| 자동 3-hint 최대 표시 지연 | %s |
				| 실행 시작 시각 | %s |
				| 실행 소요 시간 | %s |

				> 이 평가는 `RUN_OPENAI_ONE_HOUR_INTEGRATION_TEST=true`일 때만 실행됩니다.
				> 실제 OpenAI Responses API를 호출했으며, API 키는 기록하지 않습니다.

				---

				## OpenAI Live Context 요청

				- 모델: `gpt-5.6-luna`
				- Reasoning effort: `low`
				- 최대 출력 토큰: `1200`
				- 응답 형식: strict JSON Schema `meeting_live_context`
				- 호출 방식: 기존 누적 맥락 + 새 확정 전사 2개를 `input`으로 전달

				### 첫 번째 요청의 새 확정 전사

				%s

				### 마지막 요청의 새 확정 전사

				%s

				---

				## 실제 OpenAI 응답

				### 첫 번째 Live Context 응답

				%s

				### 마지막 Live Context 응답

				%s

				---

				## 실제 자동 3-hint 응답

				%s
				""".formatted(
				meetingId,
				segments.size(),
				updates.size(),
				hints.size(),
				formatDuration(eventCapture.averageLiveContextLatency(meetingId)),
				formatDuration(eventCapture.maxLiveContextLatency(meetingId)),
				formatDuration(eventCapture.averageAutoHintLatency(meetingId)),
				formatDuration(eventCapture.maxAutoHintLatency(meetingId)),
				startedAt,
				Duration.between(startedAt, Instant.now()),
				formatSegments(segments.subList(0, BATCH_SIZE)),
				formatSegments(segments.subList(segments.size() - BATCH_SIZE, segments.size())),
				formatContext(first),
				formatContext(last),
				hintSection);
	}

	private String formatSegments(List<TranscriptSegment> segments) {
		return segments.stream()
				.map(segment -> "- `segmentId=%d, sequence=%d` %s".formatted(
						segment.getId(), segment.getSequenceIndex(), segment.getContent()))
				.reduce((left, right) -> left + "\n" + right)
				.orElse("없음");
	}

	private String formatContext(LiveContextUpdatedEvent event) {
		LiveContextSnapshot context = event.context();
		return """
				- 마지막 반영 세그먼트: `%d` (sequence `%d`)
				- 누적 요약: %s
				- 현재 주제: %s
				- 결정 사항: %s
				- 액션 아이템: %s
				- 미해결 질문: %s
				- 자동 힌트 판단: `shouldGenerate=%s`, `targetSegmentId=%s`, `importance=%d`, 사유: %s
				""".formatted(
				event.lastSegmentId(), event.lastSequenceIndex(),
				context.rollingSummary(), valueOrNone(context.currentTopic()),
				listOrNone(context.decisions()), listOrNone(context.actionItems()), listOrNone(context.openQuestions()),
				event.autoHintDecision().shouldGenerate(), event.autoHintDecision().targetSegmentId(),
				event.autoHintDecision().importance(), valueOrNone(event.autoHintDecision().triggerReason()));
	}

	private String formatHint(SegmentHint hint) {
		return """
				- 대상 세그먼트: `%d`
				- 중요도: `%d` / 판단 사유: %s
				- 의미: %s
				- 내 영향: %s
				- 팀 질문: %s
				""".formatted(hint.getSegmentId(), hint.getImportance(), hint.getTriggerReason(), hint.getMeaning(),
				hint.getMyImpact(), hint.getTeamQuestion());
	}

	private String valueOrNone(String value) {
		return value == null || value.isBlank() ? "없음" : value;
	}

	private String listOrNone(List<String> values) {
		return values.isEmpty() ? "없음" : String.join(" / ", values);
	}

	private String formatDuration(Duration duration) {
		return duration == null ? "생성되지 않음" : "%dms".formatted(duration.toMillis());
	}

	@TestConfiguration
	static class EventCaptureConfig {

		@Bean
		EventCapture eventCapture() {
			return new EventCapture();
		}
	}

	static class EventCapture {

		private final List<LiveContextUpdatedEvent> liveContexts = new CopyOnWriteArrayList<>();
		private final List<AutoHintCreatedEvent> autoHints = new CopyOnWriteArrayList<>();
		private final Map<Long, Instant> finalizedAtBySegmentId = new ConcurrentHashMap<>();
		private final Map<Integer, Instant> liveContextUpdatedAtBySequence = new ConcurrentHashMap<>();
		private final Map<Long, Instant> autoHintCreatedAtBySegmentId = new ConcurrentHashMap<>();

		@EventListener
		public void record(TranscriptFinalizedEvent event) {
			finalizedAtBySegmentId.put(event.segmentId(), Instant.now());
		}

		@EventListener
		public void record(LiveContextUpdatedEvent event) {
			liveContexts.add(event);
			liveContextUpdatedAtBySequence.put(event.lastSequenceIndex(), Instant.now());
		}

		@EventListener
		public void record(AutoHintCreatedEvent event) {
			autoHints.add(event);
			autoHintCreatedAtBySegmentId.put(event.hint().getSegmentId(), Instant.now());
		}

		List<LiveContextUpdatedEvent> liveContextsFor(Long meetingId) {
			return liveContexts.stream()
					.filter(event -> event.meetingId().equals(meetingId))
					.sorted(Comparator.comparing(LiveContextUpdatedEvent::lastSequenceIndex))
					.toList();
		}

		List<AutoHintCreatedEvent> autoHintsFor(Long meetingId) {
			return new ArrayList<>(autoHints.stream()
					.filter(event -> event.meetingId().equals(meetingId))
					.toList());
		}

		Duration averageLiveContextLatency(Long meetingId) {
			return average(liveContextsFor(meetingId).stream()
					.map(event -> latency(event.lastSegmentId(), liveContextUpdatedAtBySequence.get(event.lastSequenceIndex())))
					.filter(java.util.Objects::nonNull)
					.toList());
		}

		Duration maxLiveContextLatency(Long meetingId) {
			return max(liveContextsFor(meetingId).stream()
					.map(event -> latency(event.lastSegmentId(), liveContextUpdatedAtBySequence.get(event.lastSequenceIndex())))
					.filter(java.util.Objects::nonNull)
					.toList());
		}

		Duration averageAutoHintLatency(Long meetingId) {
			return average(autoHintsFor(meetingId).stream()
					.map(event -> latency(event.hint().getSegmentId(), autoHintCreatedAtBySegmentId.get(event.hint().getSegmentId())))
					.filter(java.util.Objects::nonNull)
					.toList());
		}

		Duration maxAutoHintLatency(Long meetingId) {
			return max(autoHintsFor(meetingId).stream()
					.map(event -> latency(event.hint().getSegmentId(), autoHintCreatedAtBySegmentId.get(event.hint().getSegmentId())))
					.filter(java.util.Objects::nonNull)
					.toList());
		}

		private Duration latency(Long segmentId, Instant completedAt) {
			Instant finalizedAt = finalizedAtBySegmentId.get(segmentId);
			return finalizedAt == null || completedAt == null ? null : Duration.between(finalizedAt, completedAt);
		}

		private Duration average(List<Duration> durations) {
			return durations.isEmpty() ? null : Duration.ofMillis((long) durations.stream()
					.mapToLong(Duration::toMillis)
					.average()
					.orElseThrow());
		}

		private Duration max(List<Duration> durations) {
			return durations.stream().max(Comparator.naturalOrder()).orElse(null);
		}
	}
}
