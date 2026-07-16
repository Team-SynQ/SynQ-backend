package com.synq.backend.domain.ai.summary.mock;

import com.synq.backend.domain.ai.summary.domain.TranscriptSegment;
import com.synq.backend.domain.ai.summary.domain.TranscriptReader;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "test"})
public class MockTranscriptReader implements TranscriptReader {

	@Override
	public List<TranscriptSegment> findByMeetingId(Long meetingId) {
		return List.of(
				new TranscriptSegment("민규", "회의 후 AI 요약 API를 이번 스프린트에 구현하면 좋겠습니다."),
				new TranscriptSegment("현규", "전사와 참고자료를 함께 읽어야 요약 품질이 좋아질 것 같습니다."),
				new TranscriptSegment("민규", "API 명세 초안은 제가 작성하고, SSE 적용 시점은 다음 회의에서 정하겠습니다.")
		);
	}
}
