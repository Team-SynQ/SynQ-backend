package com.synq.backend.domain.ai.assistant.application;

import com.synq.backend.domain.ai.assistant.code.AiChatErrorCode;
import com.synq.backend.domain.ai.assistant.domain.AiChatContext;
import com.synq.backend.domain.ai.assistant.domain.AiChatMessage;
import com.synq.backend.domain.ai.assistant.domain.AiChatReference;
import com.synq.backend.domain.ai.assistant.domain.AiChatSource;
import com.synq.backend.domain.ai.assistant.domain.AiChatTranscript;
import com.synq.backend.domain.ai.assistant.domain.AiChatTurn;
import com.synq.backend.domain.ai.context.domain.LiveContextSnapshot;
import com.synq.backend.domain.ai.context.repository.LiveContextRepository;
import com.synq.backend.domain.ai.rag.search.ChunkMatch;
import com.synq.backend.domain.ai.rag.search.ChunkSearchQuery;
import com.synq.backend.domain.ai.rag.search.ChunkSearcher;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.domain.transcript.entity.TranscriptSegment;
import com.synq.backend.domain.transcript.repository.TranscriptSegmentRepository;
import com.synq.backend.domain.user.entity.RoleProfile;
import com.synq.backend.domain.user.repository.RoleProfilePerspectiveRepository;
import com.synq.backend.domain.user.repository.RoleProfileRepository;
import com.synq.backend.global.apipayload.exception.GeneralException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.PageRequest;

/**
 * Chat 질문을 답할 때 필요한 회의 데이터만 모아 AI 제공자에게 전달한다.
 */
@Component
public class AiChatContextBuilder {

	private final MeetingRepository meetingRepository;
	private final TranscriptSegmentRepository transcriptSegmentRepository;
	private final LiveContextRepository liveContextRepository;
	private final AiChatStore aiChatStore;
	private final RoleProfileRepository roleProfileRepository;
	private final RoleProfilePerspectiveRepository perspectiveRepository;
	private final ChunkSearcher chunkSearcher;
	private final AiChatProperties properties;

	public AiChatContextBuilder(
			MeetingRepository meetingRepository,
			TranscriptSegmentRepository transcriptSegmentRepository,
			LiveContextRepository liveContextRepository,
			AiChatStore aiChatStore,
			RoleProfileRepository roleProfileRepository,
			RoleProfilePerspectiveRepository perspectiveRepository,
			ChunkSearcher chunkSearcher,
			AiChatProperties properties
	) {
		this.meetingRepository = meetingRepository;
		this.transcriptSegmentRepository = transcriptSegmentRepository;
		this.liveContextRepository = liveContextRepository;
		this.aiChatStore = aiChatStore;
		this.roleProfileRepository = roleProfileRepository;
		this.perspectiveRepository = perspectiveRepository;
		this.chunkSearcher = chunkSearcher;
		this.properties = properties;
	}

	public AiChatContext build(Long meetingId, Long userId, String question, Long linkedSegmentId) {
		Meeting meeting = meetingRepository.findById(meetingId)
				.orElseThrow(() -> new GeneralException(AiChatErrorCode.CHAT_NOT_AVAILABLE));
		List<TranscriptSegment> selectedSegments = selectTranscriptSegments(meetingId, linkedSegmentId);
		List<ChunkMatch> references = chunkSearcher.search(new ChunkSearchQuery(
				meeting.getProjectId(),
				searchQuery(question, selectedSegments),
				properties.ragTopK(),
				properties.ragMinSimilarity()
		));

		RoleProfile profile = roleProfileRepository.findByUserIdAndIsDefaultTrue(userId).orElse(null);
		List<String> perspectives = profile == null ? List.of() : perspectiveRepository
				.findAllByRoleProfileId(profile.getId()).stream()
				.map(value -> value.getPerspective().name())
				.toList();

		return new AiChatContext(
				meetingId,
				userId,
				roleDescription(profile),
				perspectives,
				liveContextRepository.findByMeetingId(meetingId)
						.map(LiveContextSnapshot::from)
						.orElseGet(LiveContextSnapshot::empty),
				selectedSegments.stream().map(this::toTranscript).toList(),
				recentTurns(meetingId, userId),
				references.stream().map(this::toReference).toList(),
				sourceCandidates(selectedSegments, references)
		);
	}

	private List<TranscriptSegment> selectTranscriptSegments(
			Long meetingId,
			Long linkedSegmentId
	) {
		if (linkedSegmentId == null) {
			List<TranscriptSegment> recentSegments = new ArrayList<>(transcriptSegmentRepository
					.findByMeetingIdOrderByStartMsDescSequenceIndexDesc(
							meetingId,
							PageRequest.of(0, properties.recentTranscriptLimit())
					));
			Collections.reverse(recentSegments);
			return List.copyOf(recentSegments);
		}

		TranscriptSegment focus = transcriptSegmentRepository.findById(linkedSegmentId)
				.filter(segment -> segment.getMeetingId().equals(meetingId))
				.orElseThrow(() -> new GeneralException(AiChatErrorCode.LINKED_SEGMENT_NOT_AVAILABLE));
		int fromSequenceIndex = Math.max(0, focus.getSequenceIndex() - properties.linkedSegmentWindow());
		int toSequenceIndex = focus.getSequenceIndex() + properties.linkedSegmentWindow();
		return transcriptSegmentRepository
				.findByMeetingIdAndSequenceIndexBetweenOrderByStartMsAscSequenceIndexAsc(
						meetingId, fromSequenceIndex, toSequenceIndex);
	}

	private String searchQuery(String question, List<TranscriptSegment> transcripts) {
		String transcriptText = transcripts.stream()
				.map(TranscriptSegment::getContent)
				.reduce("", (left, right) -> left.isBlank() ? right : left + "\n" + right);
		return transcriptText.isBlank() ? question : question + "\n" + transcriptText;
	}

	private List<AiChatTurn> recentTurns(Long meetingId, Long userId) {
		if (properties.recentTurnLimit() == 0) {
			return List.of();
		}
		return aiChatStore.findHistory(meetingId, userId, 0, properties.recentTurnLimit()).getContent().stream()
				.filter(message -> message.getAnswer() != null)
				.sorted(Comparator.comparing(AiChatMessage::getCreatedAt))
				.map(message -> new AiChatTurn(message.getQuestion(), message.getAnswer()))
				.toList();
	}

	private List<AiChatSource> sourceCandidates(
			List<TranscriptSegment> transcripts,
			List<ChunkMatch> references
	) {
		List<AiChatSource> sources = new ArrayList<>();
		transcripts.forEach(segment -> sources.add(new AiChatSource(
				"TRANSCRIPT_SEGMENT", segment.getId(), "현재 회의 발화 " + segment.getSequenceIndex())));
		references.forEach(reference -> sources.add(new AiChatSource(
				"REFERENCE_MATERIAL", reference.referenceMaterialId(), "참고자료 " + reference.referenceMaterialId())));
		Map<String, AiChatSource> uniqueSources = new LinkedHashMap<>();
		sources.forEach(source -> uniqueSources.putIfAbsent(source.type() + ":" + source.id(), source));
		return List.copyOf(uniqueSources.values());
	}

	private AiChatTranscript toTranscript(TranscriptSegment segment) {
		return new AiChatTranscript(segment.getId(), segment.getSpeakerLabel(), segment.getContent());
	}

	private AiChatReference toReference(ChunkMatch reference) {
		return new AiChatReference(reference.referenceMaterialId(), reference.chunkId(), reference.content());
	}

	private String roleDescription(RoleProfile profile) {
		if (profile == null) {
			return "";
		}
		if (profile.getDetailRole() == null || profile.getDetailRole().isBlank()) {
			return profile.getRole().name();
		}
		return profile.getRole().name() + " - " + profile.getDetailRole();
	}
}
