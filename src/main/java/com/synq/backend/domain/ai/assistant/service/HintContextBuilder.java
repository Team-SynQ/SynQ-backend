package com.synq.backend.domain.ai.assistant.service;

import com.synq.backend.domain.ai.assistant.application.MemberProfile;
import com.synq.backend.domain.ai.assistant.application.MemberProfileReader;
import com.synq.backend.domain.ai.assistant.code.AssistantErrorCode;
import com.synq.backend.domain.ai.assistant.domain.HintInput;
import com.synq.backend.domain.ai.context.domain.LiveContextSnapshot;
import com.synq.backend.domain.ai.context.repository.LiveContextRepository;
import com.synq.backend.domain.ai.rag.search.ChunkMatch;
import com.synq.backend.domain.ai.rag.search.ChunkSearchQuery;
import com.synq.backend.domain.ai.rag.search.ChunkSearcher;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.domain.transcript.entity.TranscriptSegment;
import com.synq.backend.domain.transcript.repository.TranscriptSegmentRepository;
import com.synq.backend.global.apipayload.exception.GeneralException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
	private final ChunkSearcher chunkSearcher;
	private final AssistantHintProperties properties;

	@Transactional(readOnly = true)
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
				projectId, windowText(window), properties.topK(), properties.minSimilarity()));

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

	private String windowText(List<TranscriptSegment> window) {
		// 조회 범위가 항상 focus 를 포함하므로 빈 문자열이 될 수 없다.
		// ChunkSearchQuery 가 빈 질의를 거부하기 때문에 이 전제가 중요하다.
		return window.stream()
				.map(TranscriptSegment::getContent)
				.collect(Collectors.joining("\n"));
	}
}
