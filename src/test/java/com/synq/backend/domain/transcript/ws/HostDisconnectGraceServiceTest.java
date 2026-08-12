package com.synq.backend.domain.transcript.ws;

import com.synq.backend.domain.meeting.service.MeetingService;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class HostDisconnectGraceServiceTest {

	private final TaskScheduler taskScheduler = mock(TaskScheduler.class);
	private final MeetingService meetingService = mock(MeetingService.class);
	private final HostDisconnectGraceService service =
			new HostDisconnectGraceService(taskScheduler, meetingService);

	@Test
	void 연결끊김_스케줄시_유예시간_뒤에_강제종료를_호출하는_작업이_등록된다() {
		ScheduledFuture<?> future = mock(ScheduledFuture.class);
		doAnswer(invocation -> {
			Runnable task = invocation.getArgument(0);
			task.run();
			return future;
		}).when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));

		service.scheduleForceEnd(5L);

		verify(meetingService).forceEndByDisconnect(5L);
	}

	@Test
	void 취소하면_걸려있던_타이머가_취소된다() {
		ScheduledFuture<?> future = mock(ScheduledFuture.class);
		doReturn(future).when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));

		service.scheduleForceEnd(5L);
		service.cancel(5L);

		verify(future).cancel(false);
	}

	@Test
	void 걸려있지_않은_타이머를_취소해도_예외없이_지나간다() {
		service.cancel(5L);
	}

	@Test
	void 새_연결끊김을_스케줄하면_이전_타이머는_취소된다() {
		ScheduledFuture<?> firstFuture = mock(ScheduledFuture.class);
		ScheduledFuture<?> secondFuture = mock(ScheduledFuture.class);
		doReturn(firstFuture, secondFuture)
				.when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));

		service.scheduleForceEnd(5L);
		service.scheduleForceEnd(5L);

		verify(firstFuture).cancel(false);
		verify(secondFuture, never()).cancel(false);
	}
}
