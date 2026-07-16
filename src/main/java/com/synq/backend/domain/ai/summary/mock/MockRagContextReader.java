package com.synq.backend.domain.ai.summary.mock;

import com.synq.backend.domain.ai.summary.domain.RagContextReader;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MockRagContextReader implements RagContextReader {

	@Override
	public List<String> findRelevantContexts(Long meetingId, String query) {
		return List.of("프로젝트 원칙: 회의 결과에는 결정 사항, 담당자, 후속 질문을 명확히 남긴다.");
	}
}
