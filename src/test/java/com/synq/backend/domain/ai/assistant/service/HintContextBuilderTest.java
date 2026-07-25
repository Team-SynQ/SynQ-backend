package com.synq.backend.domain.ai.assistant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.synq.backend.domain.ai.assistant.domain.HintInput;
import com.synq.backend.domain.ai.assistant.mock.FakeProjectMemberPerspectivePort;
import com.synq.backend.domain.ai.assistant.mock.FakeTranscriptSegmentPort;
import com.synq.backend.domain.ai.assistant.port.MeetingProjectPort;
import com.synq.backend.domain.ai.assistant.port.ProjectMemberPerspectivePort;
import com.synq.backend.domain.ai.context.repository.LiveContextRepository;
import com.synq.backend.domain.ai.rag.search.ChunkSearchQuery;
import com.synq.backend.domain.ai.rag.search.ChunkSearcher;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HintContextBuilderTest {

	@Mock
	LiveContextRepository liveContextRepository;
	@Mock
	ChunkSearcher chunkSearcher;

	private final AssistantHintProperties properties = new AssistantHintProperties(2, 2, 3, 0.3);

	private final MeetingProjectPort meetingProjectPort = meetingId -> Optional.of(1L);

	private HintContextBuilder builder(ProjectMemberPerspectivePort perspectivePort) {
		return new HintContextBuilder(
				new FakeTranscriptSegmentPort(),
				meetingProjectPort,
				perspectivePort,
				liveContextRepository,
				chunkSearcher,
				properties);
	}

	@Test
	void 윈도우와_관점과_RAG_를_HintInput_으로_조립한다() {
		given(liveContextRepository.findByMeetingId(1L)).willReturn(Optional.empty());
		given(chunkSearcher.search(any())).willReturn(List.of());

		HintInput input = builder(new FakeProjectMemberPerspectivePort()).build(10L, 1L, 3L);

		assertThat(input.focusSegment()).isEqualTo("RAG 검색은 이미 되어 있으니 재사용하죠.");
		assertThat(input.windowBefore()).hasSize(2);
		assertThat(input.windowAfter()).hasSize(2);
		assertThat(input.role()).isEqualTo("백엔드 개발자");
		assertThat(input.liveContext()).isNotNull();
		assertThat(input.references()).isEmpty();
	}

	@Test
	void RAG_질의문은_윈도우_텍스트다() {
		given(liveContextRepository.findByMeetingId(1L)).willReturn(Optional.empty());
		given(chunkSearcher.search(any())).willReturn(List.of());

		builder(new FakeProjectMemberPerspectivePort()).build(10L, 1L, 3L);

		ArgumentCaptor<ChunkSearchQuery> captor = ArgumentCaptor.forClass(ChunkSearchQuery.class);
		Mockito.verify(chunkSearcher).search(captor.capture());
		assertThat(captor.getValue().query())
				.contains("RAG 검색은 이미 되어 있으니 재사용하죠.")
				.contains("관점 기반 개인화");
		assertThat(captor.getValue().projectId()).isEqualTo(1L);
	}

	@Test
	void 관점이_없으면_빈_역할과_관점으로_진행한다() {
		given(liveContextRepository.findByMeetingId(1L)).willReturn(Optional.empty());
		given(chunkSearcher.search(any())).willReturn(List.of());
		ProjectMemberPerspectivePort empty = (projectId, userId) -> Optional.empty();

		HintInput input = builder(empty).build(10L, 1L, 3L);

		assertThat(input.role()).isEmpty();
		assertThat(input.perspective()).isEmpty();
	}
}
