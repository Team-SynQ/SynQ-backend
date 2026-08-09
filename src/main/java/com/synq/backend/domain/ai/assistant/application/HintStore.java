package com.synq.backend.domain.ai.assistant.application;

import com.synq.backend.domain.ai.assistant.domain.HintResult;
import com.synq.backend.domain.ai.assistant.domain.SegmentHint;
import com.synq.backend.domain.ai.assistant.repository.SegmentHintRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 외부 AI 호출과 DB 트랜잭션을 분리하기 위한 3-hint 저장 경계다.
 */
@Component
@RequiredArgsConstructor
public class HintStore {

	private final SegmentHintRepository segmentHintRepository;

	/**
	 * 같은 (meeting, segment, user) 면 새 행을 쌓지 않고 덮어쓴다. 사용자가 같은 발화를
	 * 여러 번 눌러도 기록에는 마지막 결과 하나만 남아야 한다.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void save(Long meetingId, Long segmentId, Long userId, HintResult result) {
		segmentHintRepository.findByMeetingIdAndSegmentIdAndUserId(meetingId, segmentId, userId)
				.ifPresentOrElse(
						hint -> hint.overwrite(result),
						() -> segmentHintRepository.save(
								SegmentHint.of(meetingId, segmentId, userId, result))
				);
	}

	@Transactional(readOnly = true)
	public List<SegmentHint> findMyHints(Long meetingId, Long userId) {
		return segmentHintRepository.findByMeetingIdAndUserIdOrderBySegmentIdAsc(meetingId, userId);
	}
}
