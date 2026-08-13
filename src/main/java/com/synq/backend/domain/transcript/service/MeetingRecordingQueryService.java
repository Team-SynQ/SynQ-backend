package com.synq.backend.domain.transcript.service;

import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.domain.transcript.code.TranscriptErrorCode;
import com.synq.backend.domain.transcript.dto.MeetingRecordingSegmentResponse;
import com.synq.backend.domain.transcript.repository.MeetingRecordingSegmentRepository;
import com.synq.backend.domain.transcript.storage.RecordingStorage;
import com.synq.backend.global.apipayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingRecordingQueryService {

	private static final Duration PLAYBACK_URL_TTL = Duration.ofMinutes(15);

	private final MeetingRepository meetingRepository;
	private final MeetingParticipantRepository meetingParticipantRepository;
	private final MeetingRecordingSegmentRepository segmentRepository;
	private final RecordingStorage recordingStorage;

	// 조회는 회의 상태와 무관하게 가능하다 — 회의가 끝난 뒤에도 녹음은 계속 들을 수 있어야 한다.
	// 세그먼트가 아직 없으면(업로드 진행 중이거나 회의가 아직 진행 중) 빈 목록을 반환한다.
	@Transactional(readOnly = true)
	public List<MeetingRecordingSegmentResponse> findRecordings(Long meetingId, Long userId) {
		requireParticipant(meetingId, userId);

		return segmentRepository.findByMeetingIdOrderByIdAsc(meetingId).stream()
				.map(segment -> MeetingRecordingSegmentResponse.of(
						segment, recordingStorage.presignedUrl(segment.getStorageKey(), PLAYBACK_URL_TTL)))
				.toList();
	}

	private void requireParticipant(Long meetingId, Long userId) {
		if (!meetingRepository.existsById(meetingId)) {
			throw new GeneralException(TranscriptErrorCode.MEETING_NOT_FOUND);
		}
		if (!meetingParticipantRepository.existsByMeetingIdAndUserIdAndLeftAtIsNull(meetingId, userId)) {
			throw new GeneralException(TranscriptErrorCode.NOT_PARTICIPANT_TO_VIEW_RECORDINGS);
		}
	}
}
