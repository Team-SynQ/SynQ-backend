package com.synq.backend.domain.ai.summary.infrastructure;

import com.synq.backend.domain.ai.summary.infrastructure.persistence.MeetingSummaryEntity;
import com.synq.backend.domain.ai.summary.infrastructure.persistence.MeetingSummaryJpaRepository;
import com.synq.backend.domain.meeting.port.MeetingSummaryTopicsReader;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * meeting 도메인의 MeetingSummaryTopicsReader 포트를 ai.summary 도메인이 구현한 어댑터.
 */
@Component
public class MeetingSummaryTopicsReaderAdapter implements MeetingSummaryTopicsReader {

	private final MeetingSummaryJpaRepository repository;

	public MeetingSummaryTopicsReaderAdapter(MeetingSummaryJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional(readOnly = true)
	public Map<Long, List<String>> findKeyTopicsByMeetingIds(List<Long> meetingIds) {
		if (meetingIds.isEmpty()) {
			return Map.of();
		}
		// 회의당 여러 버전의 요약이 있을 수 있어(재시도 등), 가장 최신 버전만 남긴다.
		return repository.findByMeetingIdIn(meetingIds).stream()
				.collect(Collectors.groupingBy(
						MeetingSummaryEntity::getMeetingId,
						Collectors.collectingAndThen(
								Collectors.maxBy(Comparator.comparingInt(MeetingSummaryEntity::getVersion)),
								entity -> entity.map(MeetingSummaryEntity::getKeyTopics).orElseGet(List::of)
						)
				));
	}
}
