package com.synq.backend.domain.meeting.service;

import com.synq.backend.domain.meeting.code.MeetingErrorCode;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.global.apipayload.exception.GeneralException;
import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// 존재 체크(existsByProjectIdAndStatus)만으로는 막을 수 없는 동시 생성 레이스를,
// DB의 partial unique index(uq_meeting_project_active)가 실제로 막아주는지 검증한다.
// @Transactional을 쓰지 않는다 — 두 스레드가 각자 독립된 트랜잭션/커넥션으로 진짜 동시에 커밋해야
// 레이스가 재현된다.
class MeetingConcurrentCreateTest extends PostgresTestContainer {

	@Autowired
	private MeetingService meetingService;

	@Test
	void 같은_프로젝트에_동시에_생성_요청하면_하나만_성공하고_나머지는_CONCURRENT_MEETING_EXISTS로_실패한다() throws Exception {
		long projectId = System.nanoTime();
		int requestCount = 5;
		ExecutorService executor = Executors.newFixedThreadPool(requestCount);
		CountDownLatch ready = new CountDownLatch(requestCount);
		CountDownLatch start = new CountDownLatch(1);

		try {
			List<CompletableFuture<Meeting>> futures = java.util.stream.IntStream.range(0, requestCount)
					.mapToObj(i -> CompletableFuture.supplyAsync(() -> {
						ready.countDown();
						awaitUninterruptibly(start);
						return meetingService.create(projectId, 10L + i, true);
					}, executor))
					.toList();

			ready.await(5, TimeUnit.SECONDS);
			start.countDown();

			long successCount = 0;
			long conflictCount = 0;
			for (CompletableFuture<Meeting> future : futures) {
				try {
					future.join();
					successCount++;
				} catch (java.util.concurrent.CompletionException e) {
					assertThat(e.getCause()).isInstanceOfSatisfying(GeneralException.class,
							exception -> assertThat(exception.getCode())
									.isEqualTo(MeetingErrorCode.CONCURRENT_MEETING_EXISTS));
					conflictCount++;
				}
			}

			assertThat(successCount).isEqualTo(1);
			assertThat(conflictCount).isEqualTo(requestCount - 1);
		} finally {
			executor.shutdownNow();
		}
	}

	private void awaitUninterruptibly(CountDownLatch latch) {
		try {
			latch.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}
}
