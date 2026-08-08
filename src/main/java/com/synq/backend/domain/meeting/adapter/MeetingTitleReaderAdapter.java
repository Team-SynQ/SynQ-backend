package com.synq.backend.domain.meeting.adapter;

import com.synq.backend.domain.ai.summary.domain.MeetingTitleReader;
import com.synq.backend.domain.ai.summary.domain.MeetingTitleWriter;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * ai.summary 도메인의 MeetingTitleReader 포트를 meeting 도메인이 구현한 어댑터.
 */
@Component
@RequiredArgsConstructor
public class MeetingTitleReaderAdapter implements MeetingTitleReader, MeetingTitleWriter {

	private final MeetingRepository meetingRepository;

	@Override
	public Optional<String> findTitle(Long meetingId) {
		return meetingRepository.findById(meetingId).map(meeting -> meeting.getTitle());
	}

	@Override
	public void updateTitle(Long meetingId, String title) {
		meetingRepository.findById(meetingId)
				.orElseThrow(() -> new IllegalStateException("회의 제목을 저장할 회의를 찾을 수 없습니다."))
				.changeTitle(title);
	}
}
