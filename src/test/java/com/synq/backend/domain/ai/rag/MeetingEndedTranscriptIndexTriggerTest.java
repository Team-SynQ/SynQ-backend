package com.synq.backend.domain.ai.rag;

import com.synq.backend.domain.meeting.event.MeetingEndedEvent;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.domain.transcript.repository.TranscriptSegmentRepository;
import com.synq.backend.support.MeetingTranscriptTestFixture;
import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 세그먼트 조립 로직만 검증한다.
 *
 * <p>빈을 주입받지 않고 직접 생성하는 이유는 @Async 프록시를 벗기기 위해서다.
 * 프록시를 거치면 handle() 이 다른 스레드에서 돌아 검증이 실행 전에 끝난다.
 * 이벤트 발행 경로는 MeetingEndedSummaryTrigger 가 같은 방식으로 이미 동작하고 있어
 * 여기서 중복 검증하지 않는다.
 */
class MeetingEndedTranscriptIndexTriggerTest extends PostgresTestContainer {

	@Autowired
	private MeetingRepository meetingRepository;

	@Autowired
	private TranscriptSegmentRepository transcriptSegmentRepository;

	@Autowired
	private MeetingTranscriptTestFixture fixture;

	private TranscriptIndexingService indexingService;
	private MeetingEndedTranscriptIndexTrigger trigger;
	private MeetingTranscriptTestFixture.Fixture meeting;

	@BeforeEach
	void setUp() {
		meeting = fixture.create();
		indexingService = Mockito.mock(TranscriptIndexingService.class);
		trigger = new MeetingEndedTranscriptIndexTrigger(
				meetingRepository, transcriptSegmentRepository, indexingService);
	}

	@Test
	void 세그먼트를_순서대로_이어붙여_인덱싱에_넘긴다() {
		fixture.saveSegments(meeting.meetingId(), "첫 번째 발화", "두 번째 발화", "세 번째 발화");

		trigger.handle(new MeetingEndedEvent(meeting.meetingId()));

		ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
		verify(indexingService).indexAsync(
				eq(meeting.meetingId()), eq(meeting.projectId()), text.capture());

		// 화자 라벨을 넣지 않는다. speaker_label 은 항상 NULL 이다.
		assertThat(text.getValue()).isEqualTo("첫 번째 발화\n두 번째 발화\n세 번째 발화");
	}

	@Test
	void 세그먼트가_없으면_빈_문자열로_넘겨_SKIPPED_판정을_서비스에_맡긴다() {
		trigger.handle(new MeetingEndedEvent(meeting.meetingId()));

		verify(indexingService).indexAsync(meeting.meetingId(), meeting.projectId(), "");
	}

	@Test
	void 존재하지_않는_회의는_인덱싱하지_않는다() {
		trigger.handle(new MeetingEndedEvent(-1L));

		verify(indexingService, never()).indexAsync(any(), any(), any());
	}
}
