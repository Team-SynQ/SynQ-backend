package com.synq.backend.domain.ai.context.domain;

import com.synq.backend.domain.transcript.event.TranscriptFinalizedEvent;

/**
 * 회의 중 최신 맥락을 생성하는 AI 제공자 포트다.
 */
public interface LiveContextAiClient {

    LiveContextResult refresh(LiveContextSnapshot previousContext, TranscriptFinalizedEvent event);
}
