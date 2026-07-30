package com.synq.backend.domain.ai.summary.application;

import com.synq.backend.domain.ai.summary.domain.SummaryContext;
import com.synq.backend.domain.ai.summary.domain.TranscriptSegment;
import com.synq.backend.domain.ai.summary.domain.RagContextReader;
import com.synq.backend.domain.ai.summary.domain.TranscriptReader;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SummaryContextBuilder {

	private static final Logger log = LoggerFactory.getLogger(SummaryContextBuilder.class);

	private final TranscriptReader transcriptReader;
	private final RagContextReader ragContextReader;
	private final SummaryProperties properties;

	public SummaryContextBuilder(
			TranscriptReader transcriptReader,
			RagContextReader ragContextReader,
			SummaryProperties properties
	) {
		this.transcriptReader = transcriptReader;
		this.ragContextReader = ragContextReader;
		this.properties = properties;
	}

	public SummaryContext build(Long meetingId) {
		List<TranscriptSegment> segments = transcriptReader.findByMeetingId(meetingId);
		if (segments.isEmpty()) {
			throw new IllegalStateException("요약할 회의 전사가 없습니다.");
		}

		String transcript = segments.stream()
				.map(segment -> formatSegment(segment.speakerLabel(), segment.content()))
				.collect(Collectors.joining("\n"));
		log.info("회의 후 요약 입력을 구성했습니다. meetingId={}, transcriptChars={}, fallbackRequired={}",
				meetingId, transcript.length(), transcript.length() > properties.maxInputChars());
		// 전사 전체를 검색어로 사용해 회의 내용과 관련된 참고자료만 가져오도록 한다.
		List<String> referenceContexts = ragContextReader.findRelevantContexts(meetingId, transcript);

		return new SummaryContext(meetingId, transcript, referenceContexts);
	}

	private String formatSegment(String speakerLabel, String content) {
		if (speakerLabel == null || speakerLabel.isBlank()) {
			return content;
		}
		return speakerLabel + ": " + content;
	}
}
