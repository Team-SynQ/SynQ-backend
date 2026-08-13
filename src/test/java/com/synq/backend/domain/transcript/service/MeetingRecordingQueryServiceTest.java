package com.synq.backend.domain.transcript.service;

import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.domain.transcript.code.TranscriptErrorCode;
import com.synq.backend.domain.transcript.dto.MeetingRecordingSegmentResponse;
import com.synq.backend.domain.transcript.entity.MeetingRecordingSegment;
import com.synq.backend.domain.transcript.repository.MeetingRecordingSegmentRepository;
import com.synq.backend.domain.transcript.storage.RecordingStorage;
import com.synq.backend.global.apipayload.exception.GeneralException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MeetingRecordingQueryServiceTest {

	private final MeetingRepository meetingRepository = mock(MeetingRepository.class);
	private final MeetingParticipantRepository meetingParticipantRepository = mock(MeetingParticipantRepository.class);
	private final MeetingRecordingSegmentRepository segmentRepository = mock(MeetingRecordingSegmentRepository.class);
	private final RecordingStorage recordingStorage = mock(RecordingStorage.class);
	private final MeetingRecordingQueryService service = new MeetingRecordingQueryService(
			meetingRepository, meetingParticipantRepository, segmentRepository, recordingStorage);

	@Test
	void 존재하지_않는_회의면_MEETING_NOT_FOUND_예외를_발생시킨다() {
		when(meetingRepository.existsById(5L)).thenReturn(false);

		assertThatThrownBy(() -> service.findRecordings(5L, 10L))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(TranscriptErrorCode.MEETING_NOT_FOUND));
		verifyNoInteractions(meetingParticipantRepository, segmentRepository);
	}

	@Test
	void 참가자가_아니면_NOT_PARTICIPANT_TO_VIEW_RECORDINGS_예외를_발생시킨다() {
		when(meetingRepository.existsById(5L)).thenReturn(true);
		when(meetingParticipantRepository.existsByMeetingIdAndUserIdAndLeftAtIsNull(5L, 10L)).thenReturn(false);

		assertThatThrownBy(() -> service.findRecordings(5L, 10L))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(TranscriptErrorCode.NOT_PARTICIPANT_TO_VIEW_RECORDINGS));
		verifyNoInteractions(segmentRepository);
	}

	@Test
	void 세그먼트를_생성순서대로_presigned_URL과_함께_반환한다() {
		when(meetingRepository.existsById(5L)).thenReturn(true);
		when(meetingParticipantRepository.existsByMeetingIdAndUserIdAndLeftAtIsNull(5L, 10L)).thenReturn(true);

		MeetingRecordingSegment first = MeetingRecordingSegment.of(5L, "recordings/5/a.webm");
		ReflectionTestUtils.setField(first, "id", 1L);
		MeetingRecordingSegment second = MeetingRecordingSegment.of(5L, "recordings/5/b.webm");
		ReflectionTestUtils.setField(second, "id", 2L);
		when(segmentRepository.findByMeetingIdOrderByIdAsc(5L)).thenReturn(List.of(first, second));
		when(recordingStorage.presignedUrl(anyString(), any(Duration.class)))
				.thenReturn("https://example.com/url");

		List<MeetingRecordingSegmentResponse> result = service.findRecordings(5L, 10L);

		assertThat(result).hasSize(2);
		assertThat(result.get(0).segmentId()).isEqualTo(1L);
		assertThat(result.get(0).url()).isEqualTo("https://example.com/url");
		assertThat(result.get(1).segmentId()).isEqualTo(2L);
	}

	@Test
	void 세그먼트가_없으면_빈_목록을_반환한다() {
		when(meetingRepository.existsById(5L)).thenReturn(true);
		when(meetingParticipantRepository.existsByMeetingIdAndUserIdAndLeftAtIsNull(5L, 10L)).thenReturn(true);
		when(segmentRepository.findByMeetingIdOrderByIdAsc(5L)).thenReturn(List.of());

		List<MeetingRecordingSegmentResponse> result = service.findRecordings(5L, 10L);

		assertThat(result).isEmpty();
		verifyNoInteractions(recordingStorage);
	}
}
