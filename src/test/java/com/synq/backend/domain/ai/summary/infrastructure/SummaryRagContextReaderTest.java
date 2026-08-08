package com.synq.backend.domain.ai.summary.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.synq.backend.domain.ai.rag.search.ChunkMatch;
import com.synq.backend.domain.ai.rag.search.ChunkSearchQuery;
import com.synq.backend.domain.ai.rag.search.ChunkSearcher;
import com.synq.backend.domain.ai.rag.search.ChunkSource;
import com.synq.backend.domain.ai.summary.application.SummaryRagProperties;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class SummaryRagContextReaderTest {

	private final MeetingRepository meetingRepository = Mockito.mock(MeetingRepository.class);
	private final ChunkSearcher chunkSearcher = Mockito.mock(ChunkSearcher.class);
	private final SummaryRagContextReader reader = new SummaryRagContextReader(
			meetingRepository, chunkSearcher, new SummaryRagProperties(5, 0.5, 100));

	@Test
	void 참고자료와_이전_회의만_요약_근거로_반환한다() {
		Meeting meeting = Mockito.mock(Meeting.class);
		when(meeting.getId()).thenReturn(10L);
		when(meeting.getProjectId()).thenReturn(3L);
		when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
		when(chunkSearcher.search(any())).thenReturn(List.of(
				match(ChunkSource.REFERENCE_MATERIAL, 41L, "프로젝트 요구사항"),
				match(ChunkSource.MEETING_TRANSCRIPT, 10L, "현재 회의 전사"),
				match(ChunkSource.MEETING_TRANSCRIPT, 9L, "이전 회의 결정")
		));

		List<String> contexts = reader.findRelevantContexts(10L, "현재 회의 전체 전사");

		assertThat(contexts).containsExactly(
				"[참고자료 #41]\n프로젝트 요구사항",
				"[이전 회의 #9]\n이전 회의 결정"
		);
		ArgumentCaptor<ChunkSearchQuery> query = ArgumentCaptor.forClass(ChunkSearchQuery.class);
		verify(chunkSearcher).search(query.capture());
		assertThat(query.getValue())
				.extracting(ChunkSearchQuery::projectId, ChunkSearchQuery::topK,
						ChunkSearchQuery::minSimilarity, ChunkSearchQuery::excludedMeetingId)
				.containsExactly(3L, 5, 0.5, 10L);
	}

	@Test
	void 긴_전사는_검색_질의_최대_길이만_유지한다() {
		Meeting meeting = Mockito.mock(Meeting.class);
		when(meeting.getId()).thenReturn(10L);
		when(meeting.getProjectId()).thenReturn(3L);
		when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
		when(chunkSearcher.search(any())).thenReturn(List.of());
		String transcript = "앞".repeat(60) + "뒤".repeat(60);

		reader.findRelevantContexts(10L, transcript);

		ArgumentCaptor<ChunkSearchQuery> query = ArgumentCaptor.forClass(ChunkSearchQuery.class);
		verify(chunkSearcher).search(query.capture());
		assertThat(query.getValue().query())
				.hasSize(100)
				.contains("[중략]")
				.startsWith("앞")
				.endsWith("뒤");
	}

	@Test
	void RAG_검색이_실패해도_빈_문맥을_반환한다() {
		Meeting meeting = Mockito.mock(Meeting.class);
		when(meeting.getId()).thenReturn(10L);
		when(meeting.getProjectId()).thenReturn(3L);
		when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
		when(chunkSearcher.search(any())).thenThrow(new IllegalStateException("embedding unavailable"));

		assertThat(reader.findRelevantContexts(10L, "현재 회의 전사")).isEmpty();
	}

	@Test
	void 회의를_찾지_못하면_검색하지_않는다() {
		when(meetingRepository.findById(10L)).thenReturn(Optional.empty());

		assertThat(reader.findRelevantContexts(10L, "현재 회의 전사")).isEmpty();
		verify(chunkSearcher, never()).search(any());
	}

	private ChunkMatch match(ChunkSource source, Long sourceId, String content) {
		return new ChunkMatch(1L, source, sourceId, 0, content, 0.9);
	}
}
