package com.synq.backend.domain.ai.summary.infrastructure;

import com.synq.backend.domain.ai.summary.domain.RagContextReader;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 참고자료와 회의의 연결 정보가 준비되기 전까지 요약이 전사만으로 동작하게 한다.
 */
@Component
public class EmptyRagContextReader implements RagContextReader {

	@Override
	public List<String> findRelevantContexts(Long meetingId, String query) {
		return List.of();
	}
}
