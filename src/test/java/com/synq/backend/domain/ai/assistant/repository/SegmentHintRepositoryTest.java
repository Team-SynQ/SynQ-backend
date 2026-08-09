package com.synq.backend.domain.ai.assistant.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.synq.backend.domain.ai.assistant.domain.HintResult;
import com.synq.backend.domain.ai.assistant.domain.SegmentHint;
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

class SegmentHintRepositoryTest extends PostgresTestContainer {

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
	private Long segmentId;

	@BeforeEach
	void setUp() {
		segmentHintRepository.deleteAll();
		transcriptSegmentRepository.deleteAll();
		// 다른 테스트가 남긴 Live Context 와 참가자가 meeting 삭제의 FK 제약에 걸린다.
		liveContextRepository.deleteAll();
		meetingParticipantRepository.deleteAll();
		meetingRepository.deleteAll();
		userRepository.deleteAll();

		Meeting meeting = meetingRepository.save(Meeting.of(1L, "힌트 저장 테스트 회의"));
		meetingId = meeting.getId();
		segmentId = transcriptSegmentRepository.save(
				TranscriptSegment.of(meetingId, 0, 0, 900, "온보딩 이탈률 이야기를 해봅시다.")).getId();
	}

	@Test
	void 저장한_힌트를_meeting_segment_user_로_찾는다() {
		Long userId = saveUser("힌트 사용자", "hint-repo-1@synq.com");
		segmentHintRepository.save(SegmentHint.of(meetingId, segmentId, userId,
				new HintResult("의미다", "영향이다", "질문이다")));

		SegmentHint found = segmentHintRepository
				.findByMeetingIdAndSegmentIdAndUserId(meetingId, segmentId, userId)
				.orElseThrow();

		assertThat(found.getMeaning()).isEqualTo("의미다");
		assertThat(found.getMyImpact()).isEqualTo("영향이다");
		assertThat(found.getTeamQuestion()).isEqualTo("질문이다");
	}

	@Test
	void 같은_사용자가_같은_세그먼트에_두_행을_만들_수_없다() {
		Long userId = saveUser("힌트 사용자", "hint-repo-2@synq.com");
		segmentHintRepository.save(SegmentHint.of(meetingId, segmentId, userId,
				new HintResult("첫 의미", "첫 영향", "첫 질문")));

		assertThatThrownBy(() -> segmentHintRepository.saveAndFlush(
				SegmentHint.of(meetingId, segmentId, userId,
						new HintResult("둘째 의미", "둘째 영향", "둘째 질문"))))
				.isInstanceOf(Exception.class);
	}

	@Test
	void 다른_사용자는_같은_세그먼트에_각자_행을_가진다() {
		Long userA = saveUser("사용자 A", "hint-repo-a@synq.com");
		Long userB = saveUser("사용자 B", "hint-repo-b@synq.com");
		segmentHintRepository.save(SegmentHint.of(meetingId, segmentId, userA,
				new HintResult("A 의미", "A 영향", "A 질문")));
		segmentHintRepository.save(SegmentHint.of(meetingId, segmentId, userB,
				new HintResult("B 의미", "B 영향", "B 질문")));

		List<SegmentHint> aHints = segmentHintRepository
				.findByMeetingIdAndUserIdOrderBySegmentIdAsc(meetingId, userA);

		assertThat(aHints).hasSize(1);
		assertThat(aHints.get(0).getMeaning()).isEqualTo("A 의미");
	}

	@Test
	void overwrite_는_행을_늘리지_않고_내용만_바꾼다() {
		Long userId = saveUser("힌트 사용자", "hint-repo-3@synq.com");
		SegmentHint hint = segmentHintRepository.save(SegmentHint.of(meetingId, segmentId, userId,
				new HintResult("첫 의미", "첫 영향", "첫 질문")));

		hint.overwrite(new HintResult("마지막 의미", "마지막 영향", "마지막 질문"));
		segmentHintRepository.saveAndFlush(hint);

		List<SegmentHint> hints = segmentHintRepository
				.findByMeetingIdAndUserIdOrderBySegmentIdAsc(meetingId, userId);
		assertThat(hints).hasSize(1);
		assertThat(hints.get(0).getMeaning()).isEqualTo("마지막 의미");
		assertThat(hints.get(0).getMyImpact()).isEqualTo("마지막 영향");
		assertThat(hints.get(0).getTeamQuestion()).isEqualTo("마지막 질문");
	}

	private Long saveUser(String name, String email) {
		return userRepository.save(User.ofLocal(name, email, "encoded-password")).getUserId();
	}
}
