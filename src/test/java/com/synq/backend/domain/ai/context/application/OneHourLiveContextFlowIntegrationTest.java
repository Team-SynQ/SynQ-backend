package com.synq.backend.domain.ai.context.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.synq.backend.domain.ai.assistant.repository.SegmentHintRepository;
import com.synq.backend.domain.ai.context.repository.LiveContextRepository;
import com.synq.backend.domain.ai.event.LiveContextUpdatedEvent;
import com.synq.backend.domain.meeting.entity.MeetingParticipant;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.entity.ParticipantRole;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.domain.transcript.entity.TranscriptSegment;
import com.synq.backend.domain.transcript.event.TranscriptFinalizedEvent;
import com.synq.backend.domain.transcript.repository.TranscriptSegmentRepository;
import com.synq.backend.support.PostgresTestContainer;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 20초 단위 180개 세그먼트로 60분 회의를 재현한다.
 * 외부 AI 대신 fake 클라이언트를 사용하지만, DB 저장과 Spring 이벤트는 실제 구성으로 통과한다.
 */
@Import(OneHourLiveContextFlowIntegrationTest.EventCaptureConfig.class)
@TestPropertySource(properties = {
		"ai.live-context.batch.segment-count=2",
		"ai.live-context.batch.max-segments-per-request=2",
		"ai.live-context.batch.max-delay=10s",
		"ai.assistant.auto-hint.enabled=true",
		"ai.assistant.auto-hint.importance-threshold=60"
})
class OneHourLiveContextFlowIntegrationTest extends PostgresTestContainer {

	private static final int SEGMENT_COUNT = 180;
	private static final int EXPECTED_BATCH_COUNT = 90;
	private static final int EXPECTED_HINT_COUNT = 1;

	@Autowired
	private TranscriptSegmentRepository transcriptSegmentRepository;

	@Autowired
	private MeetingParticipantRepository participantRepository;

	@Autowired
	private MeetingRepository meetingRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private LiveContextRepository liveContextRepository;

	@Autowired
	private SegmentHintRepository segmentHintRepository;

	@Autowired
	private ApplicationEventPublisher eventPublisher;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private LiveContextEventCounter liveContextEventCounter;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void 한시간_회의에서_Live_Context는_90번_갱신되고_같은_주제의_자동_힌트는_중복_제거된다() {
		TestMeeting meeting = createTestMeeting();
		try {
			participantRepository.save(MeetingParticipant.of(
					meeting.meetingId(), meeting.hostId(), ParticipantRole.HOST));

			for (int sequence = 0; sequence < SEGMENT_COUNT; sequence++) {
				publishFinalizedSegment(meeting.meetingId(), sequence);
				if ((sequence + 1) % 2 == 0) {
					awaitLiveContext(meeting.meetingId(), sequence);
				}
			}

			awaitAutoHints(meeting.meetingId(), meeting.hostId(), EXPECTED_HINT_COUNT);

			assertThat(liveContextEventCounter.countFor(meeting.meetingId())).isEqualTo(EXPECTED_BATCH_COUNT);
			assertThat(segmentHintRepository.findByMeetingIdAndUserIdOrderBySegmentIdAsc(
					meeting.meetingId(), meeting.hostId())).hasSize(EXPECTED_HINT_COUNT);
			assertThat(liveContextRepository.findByMeetingId(meeting.meetingId()).orElseThrow().getLastSequenceIndex())
					.isEqualTo(SEGMENT_COUNT - 1);
		} finally {
			deleteFixture(meeting);
		}
	}

	private TestMeeting createTestMeeting() {
		String identifier = UUID.randomUUID().toString();
		User host = userRepository.saveAndFlush(
				User.ofLocal("Live Context 테스트", identifier + "@synq.com", "password-hash"));
		Meeting meeting = meetingRepository.saveAndFlush(Meeting.of(System.nanoTime(), "1시간 테스트 회의"));
		return new TestMeeting(meeting.getId(), host.getUserId());
	}

	private void deleteFixture(TestMeeting fixture) {
		jdbcTemplate.update("DELETE FROM ai_segment_hint WHERE meeting_id = ?", fixture.meetingId());
		jdbcTemplate.update("DELETE FROM meeting_live_context WHERE meeting_id = ?", fixture.meetingId());
		jdbcTemplate.update("DELETE FROM transcript_segment WHERE meeting_id = ?", fixture.meetingId());
		jdbcTemplate.update("DELETE FROM meeting_participant WHERE meeting_id = ?", fixture.meetingId());
		jdbcTemplate.update("DELETE FROM meeting WHERE id = ?", fixture.meetingId());
		jdbcTemplate.update("DELETE FROM users WHERE user_id = ?", fixture.hostId());
	}

	private record TestMeeting(Long meetingId, Long hostId) {
	}

	private void publishFinalizedSegment(Long meetingId, int sequence) {
		new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
			int startMs = sequence * 20_000;
			TranscriptSegment segment = transcriptSegmentRepository.saveAndFlush(TranscriptSegment.of(
					meetingId,
					sequence,
					startMs,
					startMs + 20_000,
					"%02d분 %02d초 발화: 이번 스프린트 진행 상황과 다음 의사결정 항목을 논의합니다."
							.formatted(startMs / 60_000, (startMs / 1_000) % 60)));
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

	private void awaitLiveContext(Long meetingId, int sequence) {
		await("Live Context", () -> liveContextRepository.findByMeetingId(meetingId)
				.map(context -> context.getLastSequenceIndex() == sequence)
				.orElse(false));
	}

	private void awaitAutoHints(Long meetingId, Long userId, int expectedCount) {
		await("자동 힌트", () -> segmentHintRepository
				.findByMeetingIdAndUserIdOrderBySegmentIdAsc(meetingId, userId)
				.size() == expectedCount);
	}

	private void await(String target, java.util.function.BooleanSupplier condition) {
		for (int attempt = 0; attempt < 300; attempt++) {
			if (condition.getAsBoolean()) {
				return;
			}
			try {
				Thread.sleep(20);
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(target + " 대기 중 인터럽트되었습니다.", exception);
			}
		}
		throw new AssertionError(target + " 갱신 시간이 초과되었습니다.");
	}

	@TestConfiguration
	static class EventCaptureConfig {

		@Bean
		LiveContextEventCounter liveContextEventCounter() {
			return new LiveContextEventCounter();
		}
	}

	static class LiveContextEventCounter {

		private final java.util.concurrent.ConcurrentHashMap<Long, AtomicInteger> counts =
				new java.util.concurrent.ConcurrentHashMap<>();

		@EventListener
		public void count(LiveContextUpdatedEvent event) {
			counts.computeIfAbsent(event.meetingId(), ignored -> new AtomicInteger()).incrementAndGet();
		}

		int countFor(Long meetingId) {
			return counts.getOrDefault(meetingId, new AtomicInteger()).get();
		}
	}
}
