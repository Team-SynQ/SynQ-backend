package com.synq.backend.domain.meeting.adapter;

import com.synq.backend.domain.ai.assistant.port.MeetingProjectPort;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 3-hint 의 RAG 검색 스코프인 projectId 를 회의로부터 해석한다.
 * 존재하지 않는 회의는 빈 값으로 돌려주고, 호출측이 MEETING_NOT_FOUND 로 처리한다.
 */
@Component
public class MeetingProjectReaderAdapter implements MeetingProjectPort {

	private final MeetingRepository meetingRepository;

	public MeetingProjectReaderAdapter(MeetingRepository meetingRepository) {
		this.meetingRepository = meetingRepository;
	}

	@Override
	public Optional<Long> findProjectId(Long meetingId) {
		return meetingRepository.findById(meetingId)
				.map(meeting -> meeting.getProjectId());
	}
}
