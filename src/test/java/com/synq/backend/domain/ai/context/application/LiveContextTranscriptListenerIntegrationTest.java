package com.synq.backend.domain.ai.context.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.synq.backend.domain.transcript.event.TranscriptFinalizedEvent;
import com.synq.backend.domain.ai.context.repository.LiveContextRepository;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.domain.transcript.entity.TranscriptSegment;
import com.synq.backend.domain.transcript.repository.TranscriptSegmentRepository;
import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class LiveContextTranscriptListenerIntegrationTest extends PostgresTestContainer {

	@Autowired
	private ApplicationEventPublisher eventPublisher;

	@Autowired
	private MeetingRepository meetingRepository;

	@Autowired
	private LiveContextRepository liveContextRepository;

	@Autowired
	private TranscriptSegmentRepository transcriptSegmentRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void Mock_확정_전사_이벤트로_회의_맥락을_생성하고_갱신한다() {
		Long meetingId = createMeetingAndPublish(0, "회의 후 AI 요약 API를 먼저 구현합시다.");
		try {
			Long lastSegmentId = publishAfterCommit(
					meetingId, 1, "전사 이벤트가 들어오면 실시간 맥락도 갱신합니다.");
			waitForLastSequence(meetingId, 1);

			var context = liveContextRepository.findByMeetingId(meetingId).orElseThrow();
			assertThat(context.getRollingSummary())
					.contains("회의 후 AI 요약 API를 먼저 구현합시다.")
					.contains("전사 이벤트가 들어오면 실시간 맥락도 갱신합니다.");
			assertThat(context.getLastSegmentId()).isEqualTo(lastSegmentId);
			assertThat(context.getLastSequenceIndex()).isEqualTo(1);
		} finally {
			jdbcTemplate.update("DELETE FROM meeting_live_context WHERE meeting_id = ?", meetingId);
			jdbcTemplate.update("DELETE FROM transcript_segment WHERE meeting_id = ?", meetingId);
			jdbcTemplate.update("DELETE FROM meeting WHERE id = ?", meetingId);
		}
	}

	private Long createMeetingAndPublish(int sequenceIndex, String content) {
		return new TransactionTemplate(transactionManager).execute(status -> {
			// project_id는 IN_PROGRESS 상태에서 유니크해야 하므로(uq_meeting_project_active), 매번 새 값을 쓴다.
			Meeting meeting = meetingRepository.save(Meeting.of(System.nanoTime(), "AI 회의"));
			publishStoredSegment(meeting.getId(), sequenceIndex, content);
			return meeting.getId();
		});
	}

	private Long publishAfterCommit(Long meetingId, int sequenceIndex, String content) {
		return new TransactionTemplate(transactionManager).execute(status ->
				publishStoredSegment(meetingId, sequenceIndex, content));
	}

	private Long publishStoredSegment(Long meetingId, int sequenceIndex, String content) {
		TranscriptSegment segment = transcriptSegmentRepository.saveAndFlush(TranscriptSegment.of(
				meetingId, sequenceIndex, sequenceIndex * 1_000, (sequenceIndex + 1) * 1_000, content));
		eventPublisher.publishEvent(new TranscriptFinalizedEvent(
				meetingId,
				segment.getId(),
				sequenceIndex,
				segment.getStartMs(),
				segment.getEndMs(),
				content,
				null));
		return segment.getId();
	}

	private void waitForLastSequence(Long meetingId, int sequenceIndex) {
		for (int attempt = 0; attempt < 100; attempt++) {
			var context = liveContextRepository.findByMeetingId(meetingId);
			if (context.isPresent() && context.get().getLastSequenceIndex() == sequenceIndex) {
				return;
			}
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Live Context 갱신 대기 중 인터럽트되었습니다.", e);
			}
		}
		throw new AssertionError("Live Context가 갱신되지 않았습니다.");
	}
}
