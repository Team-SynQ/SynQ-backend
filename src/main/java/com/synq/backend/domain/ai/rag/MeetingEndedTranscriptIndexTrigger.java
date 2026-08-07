package com.synq.backend.domain.ai.rag;

import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.event.MeetingEndedEvent;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.domain.transcript.entity.TranscriptSegment;
import com.synq.backend.domain.transcript.repository.TranscriptSegmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.stream.Collectors;

/**
 * meeting 도메인과의 결합을 MeetingEndedEvent 하나로 제한한다.
 * 회의 종료가 커밋된 뒤 전사 인덱싱을 접수한다.
 *
 * <p>MeetingEndedSummaryTrigger 와 병렬로 돈다. 전사를 각자 읽어 조회가 중복되지만,
 * 한쪽 실패가 다른 쪽을 막지 않는 것이 더 중요하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingEndedTranscriptIndexTrigger {

	private final MeetingRepository meetingRepository;
	private final TranscriptSegmentRepository transcriptSegmentRepository;
	private final TranscriptIndexingService indexingService;

	// 트랜잭션으로 묶지 않는다. AFTER_COMMIT 리스너에 @Transactional(REQUIRED) 는 금지돼 있고,
	// 두 조회가 서로 독립적인 단순 읽기라 읽기 일관성이 필요하지도 않다.
	@Async("indexingExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void handle(MeetingEndedEvent event) {
		Long projectId = meetingRepository.findById(event.meetingId())
				.map(Meeting::getProjectId)
				.orElse(null);
		if (projectId == null) {
			// 회의가 사라졌으면 인덱싱할 대상도 없다. 예외로 만들 이유가 없다.
			log.warn("회의를 찾지 못해 전사 인덱싱을 건너뜁니다. meetingId={}", event.meetingId());
			return;
		}

		// 화자 라벨을 넣지 않는다. speaker_label 은 화자분리 미구현으로 항상 NULL 이고,
		// 녹음이 회의 시작자 1명으로 한정되어 있다.
		String transcriptText = transcriptSegmentRepository
				.findByMeetingIdOrderByStartMsAscSequenceIndexAsc(event.meetingId())
				.stream()
				.map(TranscriptSegment::getContent)
				.collect(Collectors.joining("\n"));

		indexingService.indexAsync(event.meetingId(), projectId, transcriptText);
	}
}
