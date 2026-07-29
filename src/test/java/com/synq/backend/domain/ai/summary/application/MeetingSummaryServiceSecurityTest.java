package com.synq.backend.domain.ai.summary.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.synq.backend.domain.ai.summary.code.SummaryErrorCode;
import com.synq.backend.domain.ai.summary.domain.SummaryJob;
import com.synq.backend.domain.ai.summary.domain.SummaryJobStatus;
import com.synq.backend.domain.ai.summary.mock.InMemoryMeetingSummaryStore;
import com.synq.backend.domain.ai.summary.mock.InMemorySummaryJobStore;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.global.apipayload.exception.GeneralException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MeetingSummaryServiceSecurityTest {

	private final MeetingParticipantRepository participantRepository = mock(MeetingParticipantRepository.class);
	private final SummaryAccessValidator accessValidator = new SummaryAccessValidator(participantRepository);

	@Test
	void 회의_참여자가_아니면_요약_요청을_거부한다() {
		when(participantRepository.existsByMeetingIdAndUserId(1L, 7L)).thenReturn(false);
		MeetingSummaryService service = service(new InMemorySummaryJobStore());

		assertThatThrownBy(() -> service.request(1L, 7L))
				.isInstanceOf(GeneralException.class)
				.extracting("code")
				.isEqualTo(SummaryErrorCode.NOT_MEETING_PARTICIPANT);
	}

	@Test
	void 제한_시간이_지난_활성_Job을_실패로_전환하고_새_작업을_접수한다() {
		var jobStore = new InMemorySummaryJobStore();
		SummaryJob staleJob = new SummaryJob(
				UUID.randomUUID(),
				1L,
				SummaryJobStatus.PROCESSING,
				0,
				"test-model",
				"test-v1",
				null,
				Instant.now().minus(Duration.ofHours(2)),
				Instant.now().minus(Duration.ofHours(2)),
				null
		);
		jobStore.save(staleJob);

		SummaryJob newJob = service(jobStore).requestAfterMeetingEnd(1L);

		assertThat(jobStore.findById(staleJob.id()).orElseThrow().status()).isEqualTo(SummaryJobStatus.FAILED);
		assertThat(newJob.status()).isEqualTo(SummaryJobStatus.QUEUED);
	}

	private MeetingSummaryService service(InMemorySummaryJobStore jobStore) {
		SummaryJobProcessor processor = mock(SummaryJobProcessor.class);
		MeetingSummaryService service = new MeetingSummaryService(
				jobStore,
				new InMemoryMeetingSummaryStore(),
				processor,
				meetingId -> true,
				new SummaryProperties("test-model", "test-v1", 600_000, Duration.ofMinutes(30)),
				accessValidator
		);
		return service;
	}
}
