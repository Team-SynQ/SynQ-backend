package com.synq.backend.domain.ai.assistant.port;

public record TranscriptSegmentView(Long segmentId, int sequenceIndex, String content) {
}
