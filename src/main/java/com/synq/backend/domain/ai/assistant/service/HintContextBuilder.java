package com.synq.backend.domain.ai.assistant.service;

import com.synq.backend.domain.ai.personalization.MemberProfile;
import com.synq.backend.domain.ai.personalization.MemberProfileReader;
import com.synq.backend.domain.ai.assistant.code.AssistantErrorCode;
import com.synq.backend.domain.ai.assistant.domain.HintInput;
import com.synq.backend.domain.ai.context.domain.LiveContextSnapshot;
import com.synq.backend.domain.ai.context.repository.LiveContextRepository;
import com.synq.backend.domain.ai.rag.search.ChunkMatch;
import com.synq.backend.domain.ai.rag.search.ChunkSearchQuery;
import com.synq.backend.domain.ai.rag.search.DocumentChunkSearcher;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.domain.transcript.entity.TranscriptSegment;
import com.synq.backend.domain.transcript.repository.TranscriptSegmentRepository;
import com.synq.backend.global.apipayload.exception.GeneralException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 3-hint 전용 입력 조립기.
 *
 * <p>transcript / meeting / user 리포지토리를 직접 읽는다.
 * AI Chat 도 같은 방식으로 읽는다.
 */
@Component
@RequiredArgsConstructor
public class HintContextBuilder {

	private final TranscriptSegmentRepository transcriptSegmentRepository;
	private final MeetingRepository meetingRepository;
	private final MemberProfileReader memberProfileReader;
	private final LiveContextRepository liveContextRepository;
	// 3-hint 는 클릭 직후 떠야 하므로 검색 경로를 늘리지 않는다. 과거 회의 전사를 섞지 않고
	// 참고자료만 본다. 실측으로 맞춘 임계값이 전사 청크에도 맞는지 아직 검증되지 않았다.
	private final DocumentChunkSearcher chunkSearcher;
	private final AssistantHintProperties properties;

	// 트랜잭션으로 묶지 않는다. chunkSearcher 가 임베딩 API 를 호출하므로,
	// 전체를 감싸면 외부 응답을 기다리는 동안 DB 커넥션을 점유한다(open-in-view=false).
	// 네 번의 단건 조회는 서로 독립적이라 읽기 일관성이 필요하지 않다.
	public HintInput build(Long userId, Long meetingId, Long segmentId) {
		TranscriptSegment focus = transcriptSegmentRepository.findById(segmentId)
				.orElseThrow(() -> new GeneralException(AssistantErrorCode.SEGMENT_NOT_FOUND));
		if (!focus.getMeetingId().equals(meetingId)) {
			throw new GeneralException(AssistantErrorCode.SEGMENT_MEETING_MISMATCH);
		}

		Long projectId = meetingRepository.findById(meetingId)
				.map(Meeting::getProjectId)
				.orElseThrow(() -> new GeneralException(AssistantErrorCode.MEETING_NOT_FOUND));

		int focusIndex = focus.getSequenceIndex();
		// 하한만 0 으로 자른다. 상한을 넘으면 결과가 적게 나올 뿐이라 자를 필요가 없다.
		List<TranscriptSegment> window = transcriptSegmentRepository
				.findByMeetingIdAndSequenceIndexBetweenOrderByStartMsAscSequenceIndexAsc(
						meetingId,
						Math.max(0, focusIndex - properties.windowBefore()),
						focusIndex + properties.windowAfter());

		MemberProfile profile = memberProfileReader.find(projectId, userId);

		LiveContextSnapshot liveContext = liveContextRepository.findByMeetingId(meetingId)
				.map(LiveContextSnapshot::from)
				.orElseGet(LiveContextSnapshot::empty);

		List<ChunkMatch> references = chunkSearcher.search(new ChunkSearchQuery(
				projectId, searchQuery(focus, window), properties.topK(), properties.minSimilarity()));

		return new HintInput(
				focus.getContent(),
				contentsBefore(window, focusIndex),
				contentsAfter(window, focusIndex),
				profile.role(),
				profile.detailRole(),
				profile.perspectives(),
				liveContext,
				references);
	}

	private List<String> contentsBefore(List<TranscriptSegment> window, int focusIndex) {
		return window.stream()
				.filter(segment -> segment.getSequenceIndex() < focusIndex)
				.map(TranscriptSegment::getContent)
				.toList();
	}

	private List<String> contentsAfter(List<TranscriptSegment> window, int focusIndex) {
		return window.stream()
				.filter(segment -> segment.getSequenceIndex() > focusIndex)
				.map(TranscriptSegment::getContent)
				.toList();
	}

	/**
	 * 검색 질의는 프롬프트 맥락과 다른 것을 원한다. 프롬프트는 LLM 이 무관한 발화를 알아서
	 * 버리므로 넓을수록 좋지만, 질의는 벡터 하나로 압축돼 취사선택이 없으므로 좁고 초점이
	 * 선명해야 한다. 그래서 searchWindow 로 좁히고 클릭한 발화를 focusRepeat 회 반복해 가중한다.
	 *
	 * <p>focusRepeat 이 1 이상이라 질의는 비지 않는다. ChunkSearchQuery 가 빈 질의를 거부한다.
	 */
	private String searchQuery(TranscriptSegment focus, List<TranscriptSegment> window) {
		int focusIndex = focus.getSequenceIndex();

		Stream<String> repeatedFocus = IntStream.range(0, properties.focusRepeat())
				.mapToObj(index -> focus.getContent());
		Stream<String> nearby = window.stream()
				.filter(segment -> segment.getSequenceIndex() != focusIndex)
				.filter(segment -> Math.abs(segment.getSequenceIndex() - focusIndex)
						<= properties.searchWindow())
				.map(TranscriptSegment::getContent);

		return Stream.concat(repeatedFocus, nearby).collect(Collectors.joining("\n"));
	}
}
