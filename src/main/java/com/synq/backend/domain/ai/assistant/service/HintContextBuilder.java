package com.synq.backend.domain.ai.assistant.service;

import com.synq.backend.domain.ai.assistant.code.AssistantErrorCode;
import com.synq.backend.domain.ai.assistant.domain.HintInput;
import com.synq.backend.domain.ai.assistant.port.MeetingProjectPort;
import com.synq.backend.domain.ai.assistant.port.MemberPerspective;
import com.synq.backend.domain.ai.assistant.port.ProjectMemberPerspectivePort;
import com.synq.backend.domain.ai.assistant.port.TranscriptSegmentPort;
import com.synq.backend.domain.ai.assistant.port.TranscriptSegmentView;
import com.synq.backend.domain.ai.assistant.port.TranscriptWindow;
import com.synq.backend.domain.ai.context.domain.LiveContextSnapshot;
import com.synq.backend.domain.ai.context.repository.LiveContextRepository;
import com.synq.backend.domain.ai.rag.search.ChunkMatch;
import com.synq.backend.domain.ai.rag.search.ChunkSearchQuery;
import com.synq.backend.domain.ai.rag.search.ChunkSearcher;
import com.synq.backend.global.apipayload.exception.GeneralException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 3-hint 전용 입력 조립기.
 */
@Component
@RequiredArgsConstructor
public class HintContextBuilder {

	private final TranscriptSegmentPort transcriptSegmentPort;
	private final MeetingProjectPort meetingProjectPort;
	private final ProjectMemberPerspectivePort perspectivePort;
	private final LiveContextRepository liveContextRepository;
	private final ChunkSearcher chunkSearcher;
	private final AssistantHintProperties properties;

	public HintInput build(Long userId, Long meetingId, Long segmentId) {
		TranscriptWindow window = transcriptSegmentPort
				.findWindow(segmentId, properties.windowBefore(), properties.windowAfter())
				.orElseThrow(() -> new GeneralException(AssistantErrorCode.SEGMENT_NOT_FOUND));
		if (!window.meetingId().equals(meetingId)) {
			throw new GeneralException(AssistantErrorCode.SEGMENT_MEETING_MISMATCH);
		}

		Long projectId = meetingProjectPort.findProjectId(meetingId)
				.orElseThrow(() -> new GeneralException(AssistantErrorCode.MEETING_NOT_FOUND));

		MemberPerspective perspective = perspectivePort.find(projectId, userId)
				.orElse(new MemberPerspective("", ""));

		LiveContextSnapshot liveContext = liveContextRepository.findByMeetingId(meetingId)
				.map(LiveContextSnapshot::from)
				.orElseGet(LiveContextSnapshot::empty);

		String windowText = windowText(window);
		List<ChunkMatch> references = chunkSearcher.search(new ChunkSearchQuery(
				projectId, windowText, properties.topK(), properties.minSimilarity()));

		return new HintInput(
				window.focus().content(),
				window.before().stream().map(TranscriptSegmentView::content).toList(),
				window.after().stream().map(TranscriptSegmentView::content).toList(),
				perspective.role(),
				perspective.perspective(),
				liveContext,
				references);
	}

	private String windowText(TranscriptWindow window) {
		List<String> lines = new ArrayList<>();
		window.before().forEach(s -> lines.add(s.content()));
		lines.add(window.focus().content());
		window.after().forEach(s -> lines.add(s.content()));
		return String.join("\n", lines);
	}
}
