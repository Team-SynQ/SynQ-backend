package com.synq.backend.domain.meeting.port;

import java.util.UUID;

/**
 * 회의 종료 시 AI 요약 작업을 접수하는 포트.
 * 실제 Job 생성은 ai.summary 도메인이 담당한다.
 */
public interface MeetingSummaryJobCreator {

	UUID createQueuedJob(Long meetingId);
}
