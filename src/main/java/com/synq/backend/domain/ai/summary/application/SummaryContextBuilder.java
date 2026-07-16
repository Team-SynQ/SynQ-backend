package com.synq.backend.domain.ai.summary.application;

import com.synq.backend.domain.ai.summary.domain.SummaryContext;
import com.synq.backend.domain.ai.summary.domain.TranscriptSegment;
import com.synq.backend.domain.ai.summary.domain.MeetingContextReader;
import com.synq.backend.domain.ai.summary.domain.RagContextReader;
import com.synq.backend.domain.ai.summary.domain.TranscriptReader;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SummaryContextBuilder {

	private final TranscriptReader transcriptReader;
	private final MeetingContextReader meetingContextReader;
	private final RagContextReader ragContextReader;

	public SummaryContextBuilder(
			TranscriptReader transcriptReader,
			MeetingContextReader meetingContextReader,
			RagContextReader ragContextReader
	) {
		this.transcriptReader = transcriptReader;
		this.meetingContextReader = meetingContextReader;
		this.ragContextReader = ragContextReader;
	}

	public SummaryContext build(Long meetingId) {
		// 현재는 Mock Reader가 데이터를 제공한다. 이후 실제 전사/맥락/RAG 구현체로 교체한다.
		List<TranscriptSegment> segments = transcriptReader.findByMeetingId(meetingId);
		if (segments.isEmpty()) {
			throw new IllegalStateException("요약할 회의 전사가 없습니다.");
		}

		String transcript = segments.stream()
				.map(segment -> "%s: %s".formatted(segment.speakerName(), segment.content()))
				.reduce((left, right) -> left + "\n" + right)
				.orElseThrow();
		String rollingSummary = meetingContextReader.findRollingSummary(meetingId).orElse("없음");
		// 전사 전체를 검색어로 사용해 회의 내용과 관련된 참고자료만 가져오도록 한다.
		List<String> referenceContexts = ragContextReader.findRelevantContexts(meetingId, transcript);

		return new SummaryContext(meetingId, transcript, rollingSummary, referenceContexts);
	}
}
