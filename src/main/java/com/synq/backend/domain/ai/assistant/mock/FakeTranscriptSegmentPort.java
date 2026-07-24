package com.synq.backend.domain.ai.assistant.mock;

import com.synq.backend.domain.ai.assistant.port.TranscriptSegmentPort;
import com.synq.backend.domain.ai.assistant.port.TranscriptSegmentView;
import com.synq.backend.domain.ai.assistant.port.TranscriptWindow;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Record 도메인 구현 전까지 쓰는 결정적 전사 대역. meetingId 는 항상 1L,
 * segmentId 는 1-based(= sequenceIndex + 1)로 매핑한다.
 */
@Component
@Profile({"local", "test"})
public class FakeTranscriptSegmentPort implements TranscriptSegmentPort {

	private static final Long MEETING_ID = 1L;
	private static final List<String> CONTENTS = List.of(
			"이번 스프린트 목표를 먼저 정합시다.",
			"3-hint 기능을 먼저 구현하는 게 좋겠습니다.",
			"RAG 검색은 이미 되어 있으니 재사용하죠.",
			"관점 기반 개인화가 이 기능의 핵심입니다.",
			"다음 회의 전까지 API 초안을 만들어 옵시다."
	);

	@Override
	public Optional<TranscriptWindow> findWindow(Long segmentId, int before, int after) {
		int idx = (int) (segmentId - 1);
		if (idx < 0 || idx >= CONTENTS.size()) {
			return Optional.empty();
		}
		List<TranscriptSegmentView> beforeList = new ArrayList<>();
		for (int i = Math.max(0, idx - before); i < idx; i++) {
			beforeList.add(view(i));
		}
		List<TranscriptSegmentView> afterList = new ArrayList<>();
		for (int i = idx + 1; i <= Math.min(CONTENTS.size() - 1, idx + after); i++) {
			afterList.add(view(i));
		}
		return Optional.of(new TranscriptWindow(MEETING_ID, idx, view(idx), beforeList, afterList));
	}

	private TranscriptSegmentView view(int i) {
		return new TranscriptSegmentView((long) (i + 1), i, CONTENTS.get(i));
	}
}
