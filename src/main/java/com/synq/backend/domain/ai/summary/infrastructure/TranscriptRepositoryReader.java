package com.synq.backend.domain.ai.summary.infrastructure;

import com.synq.backend.domain.ai.summary.domain.TranscriptReader;
import com.synq.backend.domain.ai.summary.domain.TranscriptSegment;
import com.synq.backend.domain.transcript.repository.TranscriptSegmentRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 전사 도메인의 확정 세그먼트를 회의 후 요약 입력 형식으로 변환한다.
 */
@Component
public class TranscriptRepositoryReader implements TranscriptReader {

	private final TranscriptSegmentRepository transcriptSegmentRepository;

	public TranscriptRepositoryReader(TranscriptSegmentRepository transcriptSegmentRepository) {
		this.transcriptSegmentRepository = transcriptSegmentRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<TranscriptSegment> findByMeetingId(Long meetingId) {
		return transcriptSegmentRepository.findByMeetingIdOrderByStartMsAscSequenceIndexAsc(meetingId)
				.stream()
				.map(segment -> new TranscriptSegment(segment.getSpeakerLabel(), segment.getContent()))
				.toList();
	}
}
