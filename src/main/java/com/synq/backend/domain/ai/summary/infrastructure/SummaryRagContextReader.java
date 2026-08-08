package com.synq.backend.domain.ai.summary.infrastructure;

import com.synq.backend.domain.ai.rag.search.ChunkMatch;
import com.synq.backend.domain.ai.rag.search.ChunkSearchQuery;
import com.synq.backend.domain.ai.rag.search.ChunkSearcher;
import com.synq.backend.domain.ai.rag.search.ChunkSource;
import com.synq.backend.domain.ai.summary.application.SummaryRagProperties;
import com.synq.backend.domain.ai.summary.domain.RagContextReader;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 회의 후 요약에 참고자료와 이전 회의 전사를 보강한다.
 *
 * <p>현재 회의의 전체 전사는 이미 요약 프롬프트에 들어가므로 검색 대상에서 제외한다.
 * RAG 장애는 요약 생성 자체를 막지 않도록 빈 문맥으로 처리한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SummaryRagContextReader implements RagContextReader {

	private static final String OMISSION_MARKER = "\n[중략]\n";

	private final MeetingRepository meetingRepository;
	private final ChunkSearcher chunkSearcher;
	private final SummaryRagProperties properties;

	@Override
	public List<String> findRelevantContexts(Long meetingId, String transcript) {
		if (transcript == null || transcript.isBlank()) {
			return List.of();
		}

		return meetingRepository.findById(meetingId)
				.map(meeting -> findContexts(meeting, transcript))
				.orElseGet(() -> {
					log.warn("회의 후 요약 RAG 대상 회의를 찾지 못했습니다. meetingId={}", meetingId);
					return List.of();
				});
	}

	private List<String> findContexts(Meeting meeting, String transcript) {
		try {
			List<ChunkMatch> matches = chunkSearcher.search(new ChunkSearchQuery(
					meeting.getProjectId(),
					limitQuery(transcript),
					properties.topK(),
					properties.minSimilarity(),
					meeting.getId()
			));

			return matches.stream()
					// 검색기 구현이 바뀌어도 현재 회의가 근거 문맥으로 중복되지 않게 한 번 더 방어한다.
					.filter(match -> match.source() != ChunkSource.MEETING_TRANSCRIPT
							|| !meeting.getId().equals(match.sourceId()))
					.map(this::format)
					.toList();
		} catch (RuntimeException e) {
			log.warn("회의 후 요약 RAG 검색에 실패해 전사만으로 요약을 생성합니다. meetingId={}", meeting.getId(), e);
			return List.of();
		}
	}

	private String limitQuery(String transcript) {
		if (transcript.length() <= properties.maxQueryChars()) {
			return transcript;
		}

		int retainedChars = properties.maxQueryChars() - OMISSION_MARKER.length();
		int headChars = retainedChars / 2;
		int tailChars = retainedChars - headChars;
		return transcript.substring(0, headChars)
				+ OMISSION_MARKER
				+ transcript.substring(transcript.length() - tailChars);
	}

	private String format(ChunkMatch match) {
		return switch (match.source()) {
			case REFERENCE_MATERIAL -> "[참고자료 #%d]\n%s".formatted(match.sourceId(), match.content());
			case MEETING_TRANSCRIPT -> "[이전 회의 #%d]\n%s".formatted(match.sourceId(), match.content());
		};
	}
}
