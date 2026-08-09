package com.synq.backend.domain.ai.assistant.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.synq.backend.domain.ai.assistant.domain.HintResult;
import com.synq.backend.domain.ai.assistant.domain.SegmentHint;
import com.synq.backend.domain.ai.assistant.repository.SegmentHintRepository;
import com.synq.backend.domain.ai.context.repository.LiveContextRepository;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.domain.transcript.entity.TranscriptSegment;
import com.synq.backend.domain.transcript.repository.TranscriptSegmentRepository;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.support.PostgresTestContainer;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class HintStoreTest extends PostgresTestContainer {

	@Autowired
	private HintStore hintStore;

	@Autowired
	private SegmentHintRepository segmentHintRepository;

	@Autowired
	private TranscriptSegmentRepository transcriptSegmentRepository;

	@Autowired
	private MeetingRepository meetingRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private LiveContextRepository liveContextRepository;

	@Autowired
	private MeetingParticipantRepository meetingParticipantRepository;

	private Long meetingId;
	private Long firstSegmentId;
	private Long secondSegmentId;

	@BeforeEach
	void setUp() {
		segmentHintRepository.deleteAll();
		transcriptSegmentRepository.deleteAll();
		// 다른 테스트가 남긴 Live Context 와 참가자가 meeting 삭제의 FK 제약에 걸린다.
		liveContextRepository.deleteAll();
		meetingParticipantRepository.deleteAll();
		meetingRepository.deleteAll();
		userRepository.deleteAll();

		meetingId = meetingRepository.save(Meeting.of(1L, "힌트 저장 경계 테스트")).getId();
		firstSegmentId = transcriptSegmentRepository.save(
				TranscriptSegment.of(meetingId, 0, 0, 900, "첫 발화")).getId();
		secondSegmentId = transcriptSegmentRepository.save(
				TranscriptSegment.of(meetingId, 1, 900, 1800, "둘째 발화")).getId();
	}

	@Test
	void 같은_세그먼트를_다시_저장하면_마지막_내용만_남는다() {
		Long userId = saveUser("힌트 사용자", "hint-store-1@synq.com");

		hintStore.save(meetingId, firstSegmentId, userId, new HintResult("첫 의미", "첫 영향", "첫 질문"));
		hintStore.save(meetingId, firstSegmentId, userId, new HintResult("둘째 의미", "둘째 영향", "둘째 질문"));
		hintStore.save(meetingId, firstSegmentId, userId, new HintResult("마지막 의미", "마지막 영향", "마지막 질문"));

		List<SegmentHint> hints = hintStore.findMyHints(meetingId, userId);
		assertThat(hints).hasSize(1);
		assertThat(hints.get(0).getMeaning()).isEqualTo("마지막 의미");
		assertThat(hints.get(0).getMyImpact()).isEqualTo("마지막 영향");
		assertThat(hints.get(0).getTeamQuestion()).isEqualTo("마지막 질문");
	}

	@Test
	void 사용자끼리는_서로의_힌트를_덮어쓰지_않는다() {
		Long userA = saveUser("사용자 A", "hint-store-a@synq.com");
		Long userB = saveUser("사용자 B", "hint-store-b@synq.com");

		hintStore.save(meetingId, firstSegmentId, userA, new HintResult("A 의미", "A 영향", "A 질문"));
		hintStore.save(meetingId, firstSegmentId, userB, new HintResult("B 의미", "B 영향", "B 질문"));
		hintStore.save(meetingId, firstSegmentId, userB, new HintResult("B 마지막", "B 영향2", "B 질문2"));

		List<SegmentHint> aHints = hintStore.findMyHints(meetingId, userA);
		List<SegmentHint> bHints = hintStore.findMyHints(meetingId, userB);

		assertThat(aHints).hasSize(1);
		assertThat(aHints.get(0).getMeaning()).isEqualTo("A 의미");
		assertThat(bHints).hasSize(1);
		assertThat(bHints.get(0).getMeaning()).isEqualTo("B 마지막");
	}

	@Test
	void 서로_다른_세그먼트는_각각_저장된다() {
		Long userId = saveUser("힌트 사용자", "hint-store-2@synq.com");

		hintStore.save(meetingId, secondSegmentId, userId, new HintResult("둘째 의미", "영향", "질문"));
		hintStore.save(meetingId, firstSegmentId, userId, new HintResult("첫 의미", "영향", "질문"));

		List<SegmentHint> hints = hintStore.findMyHints(meetingId, userId);

		assertThat(hints).hasSize(2);
		assertThat(hints).extracting(SegmentHint::getSegmentId)
				.containsExactly(firstSegmentId, secondSegmentId);
	}

	@Test
	void 힌트가_없으면_빈_목록을_반환한다() {
		Long userId = saveUser("힌트 사용자", "hint-store-3@synq.com");

		assertThat(hintStore.findMyHints(meetingId, userId)).isEmpty();
	}

	private Long saveUser(String name, String email) {
		return userRepository.save(User.ofLocal(name, email, "encoded-password")).getUserId();
	}
}
