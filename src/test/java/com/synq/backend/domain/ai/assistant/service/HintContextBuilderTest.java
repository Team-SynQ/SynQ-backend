package com.synq.backend.domain.ai.assistant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.synq.backend.domain.ai.assistant.code.AssistantErrorCode;
import com.synq.backend.domain.ai.assistant.domain.HintInput;
import com.synq.backend.domain.ai.rag.search.ChunkSearchQuery;
import com.synq.backend.domain.ai.rag.search.ChunkSearcher;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.domain.transcript.entity.TranscriptSegment;
import com.synq.backend.domain.transcript.repository.TranscriptSegmentRepository;
import com.synq.backend.domain.user.entity.Perspective;
import com.synq.backend.domain.user.entity.Role;
import com.synq.backend.domain.user.entity.RoleProfile;
import com.synq.backend.domain.user.entity.RoleProfilePerspective;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.RoleProfilePerspectiveRepository;
import com.synq.backend.domain.user.repository.RoleProfileRepository;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.global.apipayload.exception.GeneralException;
import com.synq.backend.support.PostgresTestContainer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * ChunkSearcher 만 대역으로 둔다. 실제 검색은 임베딩 호출과 비어 있는 document_chunk 에 의존해
 * 항상 0 건이라 질의문이 무엇이었는지 검증할 수 없기 때문이다.
 */
@Transactional
class HintContextBuilderTest extends PostgresTestContainer {

	private static final List<String> CONTENTS = List.of(
			"이번 스프린트 목표를 먼저 정합시다.",
			"3-hint 기능을 먼저 구현하는 게 좋겠습니다.",
			"RAG 검색은 이미 되어 있으니 재사용하죠.",
			"관점 기반 개인화가 이 기능의 핵심입니다.",
			"다음 회의 전까지 API 초안을 만들어 옵시다."
	);

	@Autowired
	HintContextBuilder builder;
	@Autowired
	MeetingRepository meetingRepository;
	@Autowired
	TranscriptSegmentRepository transcriptSegmentRepository;
	@Autowired
	UserRepository userRepository;
	@Autowired
	RoleProfileRepository roleProfileRepository;
	@Autowired
	RoleProfilePerspectiveRepository perspectiveRepository;

	@MockitoBean
	ChunkSearcher chunkSearcher;

	private Long meetingId;
	private Long userId;
	private List<Long> segmentIds;

	@BeforeEach
	void setUp() {
		Mockito.reset(chunkSearcher);
		given(chunkSearcher.search(any())).willReturn(List.of());

		userId = userRepository.save(
				User.ofLocal("테스트", UUID.randomUUID() + "@synq.com", "password-hash")).getUserId();
		meetingId = meetingRepository.save(Meeting.of(7L, "3-hint 회의")).getId();

		segmentIds = new ArrayList<>();
		for (int i = 0; i < CONTENTS.size(); i++) {
			segmentIds.add(transcriptSegmentRepository.save(
					TranscriptSegment.of(meetingId, i, i * 1000, i * 1000 + 900, CONTENTS.get(i))).getId());
		}
	}

	@Test
	void 윈도우와_관점과_RAG_를_HintInput_으로_조립한다() {
		saveDefaultProfile(Role.DEV_TECH, "백엔드", Perspective.TECH_RISK);

		HintInput input = builder.build(userId, meetingId, segmentIds.get(2));

		assertThat(input.focusSegment()).isEqualTo(CONTENTS.get(2));
		assertThat(input.windowBefore()).containsExactly(CONTENTS.get(0), CONTENTS.get(1));
		assertThat(input.windowAfter()).containsExactly(CONTENTS.get(3), CONTENTS.get(4));
		assertThat(input.role()).isEqualTo("DEV_TECH");
		assertThat(input.detailRole()).isEqualTo("백엔드");
		assertThat(input.perspectives()).containsExactly("TECH_RISK");
		assertThat(input.liveContext()).isNotNull();
		assertThat(input.references()).isEmpty();
	}

	@Test
	void 앞_세그먼트가_부족하면_있는_만큼만_담는다() {
		HintInput input = builder.build(userId, meetingId, segmentIds.get(0));

		assertThat(input.windowBefore()).isEmpty();
		assertThat(input.windowAfter()).containsExactly(CONTENTS.get(1), CONTENTS.get(2));
	}

	@Test
	void 뒤_세그먼트가_부족하면_있는_만큼만_담는다() {
		HintInput input = builder.build(userId, meetingId, segmentIds.get(4));

		assertThat(input.windowBefore()).containsExactly(CONTENTS.get(2), CONTENTS.get(3));
		assertThat(input.windowAfter()).isEmpty();
	}

	@Test
	void RAG_질의문은_윈도우_텍스트다() {
		builder.build(userId, meetingId, segmentIds.get(2));

		ArgumentCaptor<ChunkSearchQuery> captor = ArgumentCaptor.forClass(ChunkSearchQuery.class);
		Mockito.verify(chunkSearcher).search(captor.capture());
		assertThat(captor.getValue().query())
				.contains(CONTENTS.get(2))
				.contains(CONTENTS.get(3));
		assertThat(captor.getValue().projectId()).isEqualTo(7L);
	}

	@Test
	void 프로필이_없으면_빈_역할과_관점으로_진행한다() {
		HintInput input = builder.build(userId, meetingId, segmentIds.get(2));

		assertThat(input.role()).isEmpty();
		assertThat(input.detailRole()).isEmpty();
		assertThat(input.perspectives()).isEmpty();
	}

	@Test
	void 없는_세그먼트면_SEGMENT_NOT_FOUND_다() {
		assertThatThrownBy(() -> builder.build(userId, meetingId, 999_999L))
				.isInstanceOf(GeneralException.class)
				.extracting("code")
				.isEqualTo(AssistantErrorCode.SEGMENT_NOT_FOUND);
	}

	@Test
	void 다른_회의의_세그먼트면_SEGMENT_MEETING_MISMATCH_다() {
		Long otherMeetingId = meetingRepository.save(Meeting.of(8L, "다른 회의")).getId();

		assertThatThrownBy(() -> builder.build(userId, otherMeetingId, segmentIds.get(2)))
				.isInstanceOf(GeneralException.class)
				.extracting("code")
				.isEqualTo(AssistantErrorCode.SEGMENT_MEETING_MISMATCH);
	}

	private void saveDefaultProfile(Role role, String detailRole, Perspective perspective) {
		RoleProfile profile = roleProfileRepository.save(RoleProfile.of(userId, role, detailRole, true));
		perspectiveRepository.save(RoleProfilePerspective.of(profile.getId(), perspective));
	}
}
