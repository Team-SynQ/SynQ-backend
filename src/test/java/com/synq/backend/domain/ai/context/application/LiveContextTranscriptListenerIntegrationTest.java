package com.synq.backend.domain.ai.context.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.synq.backend.domain.transcript.event.TranscriptFinalizedEvent;
import com.synq.backend.domain.ai.context.repository.LiveContextRepository;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
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
	private PlatformTransactionManager transactionManager;

	@Test
	void Mock_확정_전사_이벤트로_회의_맥락을_생성하고_갱신한다() {
		Long meetingId = createMeetingAndPublish(1L, 0, "회의 후 AI 요약 API를 먼저 구현합시다.");
		waitForLastSequence(meetingId, 0);
		publishAfterCommit(meetingId, 2L, 1, "전사 이벤트가 들어오면 실시간 맥락도 갱신합니다.");
		waitForLastSequence(meetingId, 1);

		var context = liveContextRepository.findByMeetingId(meetingId).orElseThrow();
		assertThat(context.getRollingSummary())
				.contains("회의 후 AI 요약 API를 먼저 구현합시다.")
				.contains("전사 이벤트가 들어오면 실시간 맥락도 갱신합니다.");
		assertThat(context.getLastSegmentId()).isEqualTo(2L);
		assertThat(context.getLastSequenceIndex()).isEqualTo(1);
	}

	private static TranscriptFinalizedEvent event(Long meetingId, Long segmentId, int sequenceIndex, String content) {
		return new TranscriptFinalizedEvent(meetingId, segmentId, sequenceIndex, 0, 1000, content, null);
	}

	private Long createMeetingAndPublish(Long segmentId, int sequenceIndex, String content) {
		return new TransactionTemplate(transactionManager).execute(status -> {
			Meeting meeting = meetingRepository.save(Meeting.of(1L, "AI 회의"));
			eventPublisher.publishEvent(event(meeting.getId(), segmentId, sequenceIndex, content));
			return meeting.getId();
		});
	}

	private void publishAfterCommit(Long meetingId, Long segmentId, int sequenceIndex, String content) {
		new TransactionTemplate(transactionManager).executeWithoutResult(status ->
				eventPublisher.publishEvent(event(meetingId, segmentId, sequenceIndex, content)));
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
