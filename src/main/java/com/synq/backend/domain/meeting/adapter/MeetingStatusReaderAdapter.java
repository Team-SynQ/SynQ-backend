package com.synq.backend.domain.meeting.adapter;

import com.synq.backend.domain.ai.summary.domain.MeetingStatusReader;
import com.synq.backend.domain.meeting.entity.MeetingStatus;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import org.springframework.stereotype.Component;

/**
 * ai.summary 도메인의 MeetingStatusReader 포트를 meeting 도메인이 구현한 어댑터.
 * 존재하지 않는 회의는 요약 대상이 아니므로 false(종료 아님)로 취급한다.
 */
@Component
public class MeetingStatusReaderAdapter implements MeetingStatusReader {

	private final MeetingRepository meetingRepository;

	public MeetingStatusReaderAdapter(MeetingRepository meetingRepository) {
		this.meetingRepository = meetingRepository;
	}

	@Override
	public boolean isEnded(Long meetingId) {
		return meetingRepository.findById(meetingId)
				.map(meeting -> meeting.getStatus() != MeetingStatus.IN_PROGRESS)
				.orElse(false);
	}
}
