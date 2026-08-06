package com.synq.backend.domain.transcript.service;

import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.entity.MeetingStatus;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.domain.transcript.code.TranscriptErrorCode;
import com.synq.backend.domain.transcript.entity.TranscriptSegment;
import com.synq.backend.domain.transcript.event.TranscriptFinalizedEvent;
import com.synq.backend.domain.transcript.repository.TranscriptSegmentRepository;
import com.synq.backend.global.apipayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TranscriptSegmentService {

	private final TranscriptSegmentRepository transcriptSegmentRepository;
	private final MeetingRepository meetingRepository;
	private final MeetingParticipantRepository meetingParticipantRepository;
	private final ApplicationEventPublisher eventPublisher;

	/**
	 * 확정 세그먼트를 저장하고 다른 도메인에 이벤트로 알린다.
	 *
	 * <p>@Transactional 이 필수다. LiveContextTranscriptListener 가
	 * {@code @TransactionalEventListener(AFTER_COMMIT)} 이라, 트랜잭션 없이 발행하면
	 * 저장이 확정되기 전에 AI 갱신이 먼저 도는 순서가 된다.
	 *
	 * <p>이 메서드는 Soniox 리더 스레드에서 호출된다. 여기서 오래 걸리면 STT 스트림이 밀리므로
	 * 저장과 발행만 하고 끝낸다. 실제 AI 갱신은 리스너 쪽 @Async 로 분리돼 있다.
	 */
	@Transactional
	public TranscriptSegment finalizeSegment(Long meetingId, int sequenceIndex, int startMs, int endMs, String content) {
		TranscriptSegment saved = transcriptSegmentRepository.save(
				TranscriptSegment.of(meetingId, sequenceIndex, startMs, endMs, content));

		eventPublisher.publishEvent(new TranscriptFinalizedEvent(
				meetingId,
				saved.getId(),
				saved.getSequenceIndex(),
				saved.getStartMs(),
				saved.getEndMs(),
				saved.getContent(),
				// 화자분리 MVP 미구현. placeholder 치환은 AI 어댑터 매핑에서만 한다.
				null
		));
		return saved;
	}

	/** 재연결해도 순번이 이어지도록 기존 최대 순번 다음 값부터 시작한다. */
	@Transactional(readOnly = true)
	public int nextSequenceIndex(Long meetingId) {
		return transcriptSegmentRepository.findMaxSequenceByMeetingId(meetingId).orElse(-1) + 1;
	}

	// 오타/오인식 교정. 호스트 제한 없이 회의에 남아있는 참가자면 누구나 수정할 수 있다.
	// 회의가 끝나면(IN_PROGRESS 가 아니면) 더 이상 수정할 수 없다 — summary retry 로 우회 반영하는 방식은 채택하지 않았다.
	@Transactional
	public TranscriptSegment update(Long meetingId, Long segmentId, Long userId, String content) {
		Meeting meeting = requireParticipant(meetingId, userId);
		if (meeting.getStatus() != MeetingStatus.IN_PROGRESS) {
			throw new GeneralException(TranscriptErrorCode.SEGMENT_EDIT_NOT_ALLOWED);
		}

		TranscriptSegment segment = transcriptSegmentRepository.findByIdAndMeetingId(segmentId, meetingId)
				.orElseThrow(() -> new GeneralException(TranscriptErrorCode.SEGMENT_NOT_FOUND));
		segment.editContent(content);
		return segment;
	}

	// 조회는 회의 상태와 무관하게 가능하다 — 회의가 끝난 뒤에도 전사는 계속 봐야 한다.
	// editContent() 가 세그먼트를 in-place 로 덮어쓰므로, 그냥 최신 row 를 읽으면 수정본이 그대로 반영된다.
	@Transactional(readOnly = true)
	public List<TranscriptSegment> getSegments(Long meetingId, Long userId) {
		requireParticipant(meetingId, userId);
		return transcriptSegmentRepository.findByMeetingIdOrderByStartMsAscSequenceIndexAsc(meetingId);
	}

	private Meeting requireParticipant(Long meetingId, Long userId) {
		Meeting meeting = meetingRepository.findById(meetingId)
				.orElseThrow(() -> new GeneralException(TranscriptErrorCode.MEETING_NOT_FOUND));
		if (!meetingParticipantRepository.existsByMeetingIdAndUserIdAndLeftAtIsNull(meetingId, userId)) {
			throw new GeneralException(TranscriptErrorCode.NOT_PARTICIPANT);
		}
		return meeting;
	}
}
