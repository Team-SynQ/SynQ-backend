package com.synq.backend.domain.ai.summary.domain;

import java.util.List;

public interface RagContextReader {
	List<String> findRelevantContexts(Long meetingId, String query);
}
