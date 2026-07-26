package com.synq.backend.domain.ai.assistant.port;

import java.util.Optional;

/**
 * 전사 세그먼트를 읽는다. Record 도메인이 구현한다. 구현 전까지 Fake 로 개발한다.
 * summary 의 TranscriptReader 와 달리 segmentId 단건과 앞뒤 윈도우를 준다.
 */
public interface TranscriptSegmentPort {

	Optional<TranscriptWindow> findWindow(Long segmentId, int before, int after);
}
