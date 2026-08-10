package com.synq.backend.domain.meeting.port;

import java.util.List;
import java.util.Map;

/**
 * 회의 목록에 표시할 주제 태그(keyTopics)를 조회하는 포트.
 * 구현(어댑터)은 ai.summary 도메인에서 제공한다.
 */
public interface MeetingSummaryTopicsReader {

	/** 요약이 아직 생성되지 않은 meetingId는 반환 맵에 포함되지 않는다. */
	Map<Long, List<String>> findKeyTopicsByMeetingIds(List<Long> meetingIds);
}
