package com.synq.backend.domain.ai.assistant.application;

import com.synq.backend.domain.ai.assistant.domain.HintResult;
import com.synq.backend.domain.ai.assistant.domain.HintSource;
import com.synq.backend.domain.ai.assistant.domain.SegmentHint;
import com.synq.backend.domain.ai.assistant.repository.SegmentHintRepository;
import java.util.List;
import java.util.Optional;
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

	/**
	 * 자동 생성은 이미 존재하는 수동 힌트를 덮어쓰지 않는다. 같은 전사 이벤트가 중복 전달된
	 * 경우에도 최초 자동 힌트 하나만 유지한다.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Optional<SegmentHint> saveAutomatically(
			Long meetingId,
			Long segmentId,
			Long userId,
			HintResult result,
			int importance,
			String triggerReason,
			String topic
	) {
		if (segmentHintRepository.existsByMeetingIdAndSegmentIdAndUserId(meetingId, segmentId, userId)) {
			return Optional.empty();
		}
		return Optional.of(segmentHintRepository.save(SegmentHint.autoOf(
				meetingId, segmentId, userId, result, importance, triggerReason, topic)));
	}

	public Optional<SegmentHint> saveAutomatically(
			Long meetingId,
			Long segmentId,
			Long userId,
			HintResult result,
			int importance,
			String triggerReason
	) {
		return saveAutomatically(meetingId, segmentId, userId, result, importance, triggerReason, null);
	}

	@Transactional(readOnly = true)
	public boolean hasRecentAutomaticDuplicate(Long meetingId, Long userId, String topic, String triggerReason) {
		return segmentHintRepository.findTop5ByMeetingIdAndUserIdAndSourceOrderByCreatedAtDesc(
				meetingId, userId, HintSource.AUTO)
				.stream()
				.anyMatch(hint -> same(topic, hint.getTopic()) && same(triggerReason, hint.getTriggerReason()));
	}

	private boolean same(String left, String right) {
		return normalize(left).equals(normalize(right));
	}

	private String normalize(String value) {
		return value == null ? "" : value.replaceAll("\\s+", " ").trim().toLowerCase();
	}

	@Transactional(readOnly = true)
	public List<SegmentHint> findMyHints(Long meetingId, Long userId) {
		return segmentHintRepository.findByMeetingIdAndUserIdOrderBySegmentIdAsc(meetingId, userId);
	}
}
